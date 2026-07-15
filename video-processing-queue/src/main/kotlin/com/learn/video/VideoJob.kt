package com.learn.video

import io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanionBase
import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "video_jobs")
class VideoJob : PanacheEntityBase {

    @Id
    @GeneratedValue
    lateinit var id: UUID

    @Column(name = "input_url", nullable = false)
    lateinit var inputUrl: String

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: VideoJobStatus = VideoJobStatus.QUEUED

    @Column(name = "output_url")
    var outputUrl: String? = null

    @Column(nullable = false)
    var attempts: Int = 0

    @Column(name = "error_message")
    var errorMessage: String? = null

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    lateinit var createdAt: Instant

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    lateinit var updatedAt: Instant

    fun toResponse(): VideoJobResponse = VideoJobResponse(
        id = id,
        inputUrl = inputUrl,
        status = status,
        outputUrl = outputUrl,
        attempts = attempts,
        errorMessage = errorMessage,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    companion object : PanacheCompanionBase<VideoJob, UUID>
}
