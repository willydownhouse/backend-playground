package com.learn.notification

import com.learn.post.PostCreatedEvent
import com.learn.user.User
import io.smallrye.common.annotation.Blocking
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import org.eclipse.microprofile.reactive.messaging.Incoming
import org.jboss.logging.Logger

@ApplicationScoped
class PostCreatedConsumer {

    private val log = Logger.getLogger(PostCreatedConsumer::class.java)

    @Incoming("post-created-in")
    @Blocking
    @Transactional
    fun consume(event: PostCreatedEvent) {
        val author = User.findById(event.authorId)
        if (author == null) {
            log.warn("Skipping fan-out: author not found authorId=${event.authorId}")
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
