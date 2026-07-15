package com.learn.post

import java.time.Instant
import java.util.UUID

data class PostCreatedEvent(
    val postId: UUID,
    val authorId: UUID,
    val authorUsername: String,
    val title: String,
    val createdAt: Instant,
)
