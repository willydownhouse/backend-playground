package com.learn.user

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import java.util.UUID

data class CreateUserRequest(
    @field:NotBlank val username: String,
    @field:NotBlank @field:Email val email: String,
)

data class UserResponse(
    val id: UUID,
    val username: String,
    val email: String,
)
