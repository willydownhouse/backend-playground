package com.learn.notification

import com.learn.post.PostCreatedEvent
import com.learn.user.User
import io.smallrye.common.annotation.Blocking
import io.smallrye.faulttolerance.api.ExponentialBackoff
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import org.eclipse.microprofile.faulttolerance.Retry
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
 * the offset is committed and the message won't be redelivered.
 *
 * Transient failures (thrown exceptions) are retried via @Retry, then sent to post-created-dlq
 * by the incoming channel failure-strategy when retries are exhausted.
 */
@ApplicationScoped
class PostCreatedConsumer(
    @Channel("post-created-dlq-out")
    private val deadLetterEmitter: Emitter<PostCreatedDeadLetter>,
    private val notificationPersister: NotificationPersister,
) {

    private val log = Logger.getLogger(PostCreatedConsumer::class.java)

    // --- Transient failure: infra errors (DB down, network timeout) ---
    //
    // Problem: something outside our control fails temporarily — the DB is unreachable,
    // a connection times out, etc. The work might succeed if we try again in a few seconds.
    //
    // Wrong approaches:
    //   - no retry → one blip loses the fan-out until Kafka redelivers (uncontrolled timing)
    //   - retry forever → hammers a struggling DB, blocks the consumer on poison messages
    //   - throw with failure-strategy=fail → stops the whole application
    //
    // Our approach: @Retry + @ExponentialBackoff, then failure-strategy=dead-letter-queue on the channel.
    //   - @Retry: up to 3 retries; initial delay 1s, then 2s, then 4s (factor 2)
    //   - still failing → message nacked → framework writes raw PostCreatedEvent to post-created-dlq
    //   - idempotent fan-out (Step 2b) makes retries and redelivery safe (no duplicate rows)
    //
    // Note: author-missing is NOT retried — that path returns successfully after our manual DLQ.
    @Incoming("post-created-in")
    @Blocking // DB work runs on a worker thread, not the event loop
    @Retry(delay = 1000, maxRetries = 3)
    @ExponentialBackoff(factor = 2, maxDelay = 10000)
    @Transactional // wraps reads; each notification insert commits in its own REQUIRES_NEW transaction
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
