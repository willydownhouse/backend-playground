package com.learn.video

import io.quarkus.panache.common.Sort
import jakarta.transaction.Transactional
import jakarta.validation.Valid
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

@Path("/videos/jobs")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class VideoJobResource {

    @GET
    @Transactional
    fun list(): List<VideoJobResponse> =
        VideoJob.findAll(Sort.descending("createdAt")).list().map { it.toResponse() }

    @DELETE
    @Transactional
    fun deleteAll(): Response {
        VideoJob.deleteAll()
        return Response.noContent().build()
    }

    @POST
    @Transactional
    fun create(@Valid request: CreateVideoJobRequest): Response {
        val job = VideoJob().apply {
            inputUrl = request.inputUrl
        }
        job.persistAndFlush()

        return Response.status(Response.Status.CREATED)
            .entity(job.toResponse())
            .build()
    }
}
