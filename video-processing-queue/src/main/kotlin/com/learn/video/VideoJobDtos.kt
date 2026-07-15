package com.learn.video

import jakarta.validation.constraints.NotBlank
import org.hibernate.validator.constraints.URL
import java.time.Instant
import java.util.UUID

data class CreateVideoJobRequest(
    @field:NotBlank
    @field:URL
    val inputUrl: String,
)

data class VideoJobResponse(
    val id: UUID,
    val inputUrl: String,
    val status: VideoJobStatus,
    val outputUrl: String?,
    val attempts: Int,
    val errorMessage: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
)
