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

@ApplicationScoped
class PostCreatedConsumer(
    @Channel("post-created-dlq-out")
    private val deadLetterEmitter: Emitter<PostCreatedDeadLetter>,
) {

    private val log = Logger.getLogger(PostCreatedConsumer::class.java)

    @Incoming("post-created-in")
    @Blocking
    @Transactional
    fun consume(event: PostCreatedEvent) {
        val author = User.findById(event.authorId)
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

        val otherUsers = User.find("id != ?1", event.authorId).list()

        for (user in otherUsers) {
            Notification().apply {
                recipient = user
                actor = author
                type = NotificationType.NEW_POST
                title = "${event.authorUsername} published a new post"
                body = event.title
                referenceId = event.postId
            }.persist()
        }

        log.info(
            "Created ${otherUsers.size} notifications for post id=${event.postId} authorId=${event.authorId}",
        )
    }
}
