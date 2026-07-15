package com.learn.video

import jakarta.validation.Valid
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

@Path("/videos/jobs")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class VideoJobResource {

    @POST
    fun create(@Valid request: CreateVideoJobRequest): Response =
        Response.status(Response.Status.CREATED)
            .entity(CreateVideoJobResponse(inputUrl = request.inputUrl))
            .build()
}
