package com.learn.notification

import com.learn.post.PostCreatedEvent
import com.learn.user.User
import io.smallrye.common.annotation.Blocking
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import org.eclipse.microprofile.reactive.messaging.Channel
import org.eclipse.microprofile.reactive.messaging.Emitter
import org.eclipse.microprofile.reactive.messaging.Incoming
import org.jboss.logging.Logger
import java.time.Instant

/**
 * Kafka consumer for post-created events.
 *
 * Flow: post-created topic → fan-out notification rows to every user except the author.
 *
 * Kafka tracks progress per consumer group via offsets. If this method returns normally,
 * the offset is committed and the message won't be redelivered. If it throws, the offset
 * is not committed and Kafka may deliver the same message again (at-least-once delivery).
 */
@ApplicationScoped
class PostCreatedConsumer(
    @Channel("post-created-dlq-out")
    private val deadLetterEmitter: Emitter<PostCreatedDeadLetter>,
    private val notificationPersister: NotificationPersister,
) {

    private val log = Logger.getLogger(PostCreatedConsumer::class.java)

    @Incoming("post-created-in")
    @Blocking // DB work runs on a worker thread, not the event loop
    @Transactional // fan-out inserts succeed or roll back together; a thrown error rolls back all rows
    fun consume(event: PostCreatedEvent) {
        val author = User.findById(event.authorId)

        // --- Permanent failure: bad data (author missing) ---
        //
        // Problem: the event references an authorId that no longer exists (deleted user,
        // stale message, data bug). Retrying will never succeed — the author won't magically appear.
        //
        // Wrong approaches:
        //   - throw → Kafka treats it as failure, redelivers forever (poison message)
        //   - return with no DLQ → message is acked and lost silently (no audit trail)
        //
        // Our approach: publish to post-created-dlq, then return successfully.
        //   - Kafka commits the offset (move on to next messages)
        //   - the failed event is preserved for inspection / manual replay
        //   - reason AUTHOR_NOT_FOUND makes the cause explicit in the DLQ payload
        if (author == null) {
            deadLetterEmitter.send(
                PostCreatedDeadLetter(
                    originalEvent = event,
                    reason = DeadLetterReason.AUTHOR_NOT_FOUND,
                    failedAt = Instant.now(),
                    sourceTopic = PostCreatedDeadLetter.SOURCE_TOPIC,
                ),
            )

            log.warn(
                "Sent dead letter for post id=${event.postId} authorId=${event.authorId} reason=${DeadLetterReason.AUTHOR_NOT_FOUND}",
            )
            return
        }

        // --- Idempotent fan-out (Step 2b) ---
        //
        // Problem: Kafka delivers at-least-once. If we insert notifications and commit to DB,
        // then crash before Kafka commits the offset, the same PostCreatedEvent is redelivered.
        // Without protection, Bob and Carol would get duplicate notifications.
        //
        // Wrong approaches:
        //   - hope Kafka never redelivers → not guaranteed
        //   - SELECT before INSERT in a loop → extra round trips; two workers can still race
        //     (both SELECT "not exists", both INSERT → duplicate)
        //
        // Our approach: unique DB constraint on (recipient_id, reference_id, type) + insert.
        //   - first delivery: INSERT succeeds
        //   - redelivery: INSERT hits constraint → treat as "already done", skip safely
        //   - each insert runs in REQUIRES_NEW so one duplicate does not abort the whole batch
        //     (PostgreSQL aborts the current transaction on constraint violation)
        //
        // The constraint is the source of truth; duplicate key means this event was already
        // processed for that recipient.
        val otherUsers = User.find("id != ?1", event.authorId).list()

        for (user in otherUsers) {
            notificationPersister.persistIfAbsent(
                Notification().apply {
                    recipient = user
                    actor = author
                    type = NotificationType.NEW_POST
                    title = "${event.authorUsername} published a new post"
                    body = event.title
                    referenceId = event.postId
                },
            )
        }

        log.info(
            "Fan-out complete for post id=${event.postId} authorId=${event.authorId} recipients=${otherUsers.size}",
        )
    }
}
