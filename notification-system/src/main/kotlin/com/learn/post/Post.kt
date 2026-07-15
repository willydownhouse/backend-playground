package com.learn.post

import com.learn.user.User
import io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanionBase
import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "posts")
class Post : PanacheEntityBase {

    @Id
    @GeneratedValue
    lateinit var id: UUID

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    lateinit var author: User

    @Column(nullable = false)
    lateinit var title: String

    @Column(nullable = false)
    lateinit var body: String

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    lateinit var createdAt: Instant

    fun toResponse(): PostResponse = PostResponse(
        id = id,
        authorId = author.id,
        title = title,
        body = body,
        createdAt = createdAt,
    )

    fun toCreatedEvent(): PostCreatedEvent = PostCreatedEvent(
        postId = id,
        authorId = author.id,
        authorUsername = author.username,
        title = title,
        createdAt = createdAt,
    )

    companion object : PanacheCompanionBase<Post, UUID>
}
