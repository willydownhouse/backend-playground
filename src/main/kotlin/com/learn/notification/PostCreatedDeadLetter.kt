package com.learn.notification

import com.learn.post.PostCreatedEvent
import java.time.Instant

data class PostCreatedDeadLetter(
    val originalEvent: PostCreatedEvent,
    val reason: DeadLetterReason,
    val failedAt: Instant,
    val sourceTopic: String,
) {
    companion object {
        const val SOURCE_TOPIC = "post-created"
    }
}
