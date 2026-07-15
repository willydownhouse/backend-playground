package com.learn.api

import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

object ApiErrors {

    fun badRequest(message: String): WebApplicationException =
        of(Response.Status.BAD_REQUEST, message)

    fun conflict(message: String): WebApplicationException =
        of(Response.Status.CONFLICT, message)

    fun notFound(message: String): WebApplicationException =
        of(Response.Status.NOT_FOUND, message)

    private fun of(status: Response.Status, message: String): WebApplicationException =
        WebApplicationException(
            Response.status(status)
                .entity(ApiError(status.statusCode, message))
                .type(MediaType.APPLICATION_JSON)
                .build(),
        )
}
