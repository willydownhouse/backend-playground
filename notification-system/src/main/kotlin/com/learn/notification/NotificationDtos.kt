package com.learn.notification

import java.time.Instant
import java.util.UUID

data class NotificationResponse(
    val id: UUID,
    val recipientId: UUID,
    val actorId: UUID,
    val type: NotificationType,
    val title: String,
    val body: String,
    val referenceId: UUID,
    val read: Boolean,
    val createdAt: Instant,
)
