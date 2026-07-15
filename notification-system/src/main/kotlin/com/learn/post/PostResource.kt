package com.learn.post

import com.learn.api.ApiErrors
import com.learn.notification.NotificationService
import com.learn.user.User
import io.quarkus.panache.common.Sort
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

@Path("/posts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class PostResource(
    private val notificationService: NotificationService,
) {

    private val log = Logger.getLogger(PostResource::class.java)

    @POST
    @Transactional
    fun create(@Valid request: CreatePostRequest): Response {
        val author = User.findById(request.authorId)
            ?: throw ApiErrors.badRequest("Invalid authorId")

        log.info("Creating post for authorId=${author.id} title=${request.title}")

        val post = Post().apply {
            this.author = author
            title = request.title
            body = request.body
        }

        post.persistAndFlush()
        notificationService.onPostCreated(post)

        log.info("Created post id=${post.id} authorId=${author.id} title=${post.title}")

        return Response.status(Response.Status.CREATED).entity(post.toResponse()).build()
    }

    @GET
    @Transactional
    fun list(): List<PostResponse> =
        Post.findAll(Sort.descending("createdAt")).list().map { it.toResponse() }
}
