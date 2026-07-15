package com.learn.notification

import com.learn.post.Post
import com.learn.post.PostCreatedEvent
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.reactive.messaging.Channel
import org.eclipse.microprofile.reactive.messaging.Emitter
import org.jboss.logging.Logger

@ApplicationScoped
class NotificationService(
    @Channel("post-created-out")
    private val postCreatedEmitter: Emitter<PostCreatedEvent>,
) {

    private val log = Logger.getLogger(NotificationService::class.java)

    fun onPostCreated(post: Post) {
        val postEvent = post.toCreatedEvent()
        postCreatedEmitter.send(postEvent)

        log.info("Published PostCreatedEvent for post id=${postEvent.postId}")
    }
}
