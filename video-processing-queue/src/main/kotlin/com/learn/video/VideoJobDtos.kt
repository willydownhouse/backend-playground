package com.learn.video

import jakarta.validation.constraints.NotBlank

data class CreateVideoJobRequest(
    @field:NotBlank val inputUrl: String,
)

data class CreateVideoJobResponse(
    val inputUrl: String,
)
