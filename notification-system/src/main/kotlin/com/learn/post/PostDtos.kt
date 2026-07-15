package com.learn.post

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.Instant
import java.util.UUID

data class CreatePostRequest(
    @field:NotNull val authorId: UUID,
    @field:NotBlank val title: String,
    @field:NotBlank val body: String,
)

data class PostResponse(
    val id: UUID,
    val authorId: UUID,
    val title: String,
    val body: String,
    val createdAt: Instant,
)
