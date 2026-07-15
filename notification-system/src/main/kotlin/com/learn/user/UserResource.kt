package com.learn.user

import com.learn.api.ApiErrors
import jakarta.transaction.Transactional
import jakarta.validation.Valid
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.jboss.logging.Logger


@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class UserResource {

    private val log = Logger.getLogger(UserResource::class.java)

    @POST
    @Transactional
    fun create(@Valid request: CreateUserRequest): Response {
        if (User.count("username", request.username) > 0) {
            throw ApiErrors.conflict("Username already exists")
        }
        if (User.count("email", request.email) > 0) {
            throw ApiErrors.conflict("Email already exists")
        }

        val user = User().apply {
            username = request.username
            email = request.email
        }



        user.persist()

        log.info("Created user id=${user.id} username=${user.username} email=${user.email}")

        return Response.status(Response.Status.CREATED).entity(user.toResponse()).build()
    }

    @GET
    fun list(): List<UserResponse> = User.listAll().map { it.toResponse() }
}
