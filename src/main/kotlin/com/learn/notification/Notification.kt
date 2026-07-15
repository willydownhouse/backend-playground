package com.learn.notification

import com.learn.user.User
import io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanionBase
import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.CreationTimestamp
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "notifications",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_notification_recipient_reference_type",
            columnNames = ["recipient_id", "reference_id", "type"],
        ),
    ],
)
class Notification : PanacheEntityBase {

    @Id
    @GeneratedValue
    lateinit var id: UUID

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_id", nullable = false)
    lateinit var recipient: User

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actor_id", nullable = false)
    lateinit var actor: User

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    lateinit var type: NotificationType

    @Column(nullable = false)
    lateinit var title: String

    @Column(nullable = false)
    lateinit var body: String

    @Column(name = "reference_id", nullable = false)
    lateinit var referenceId: UUID

    @Column(nullable = false)
    var read: Boolean = false

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    lateinit var createdAt: Instant

    fun toResponse(): NotificationResponse = NotificationResponse(
        id = id,
        recipientId = recipient.id,
        actorId = actor.id,
        type = type,
        title = title,
        body = body,
        referenceId = referenceId,
        read = read,
        createdAt = createdAt,
    )

    companion object : PanacheCompanionBase<Notification, UUID>
}
