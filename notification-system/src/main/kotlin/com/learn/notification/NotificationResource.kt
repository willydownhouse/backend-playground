package com.learn.notification

import com.learn.api.ApiErrors
import com.learn.user.User
import io.quarkus.panache.common.Sort
import jakarta.transaction.Transactional
import jakarta.ws.rs.GET
import jakarta.ws.rs.PATCH
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType
import java.util.UUID
import org.jboss.logging.Logger

@Path("/notifications")
@Produces(MediaType.APPLICATION_JSON)
class NotificationResource {

    private val log = Logger.getLogger(NotificationResource::class.java)

    @GET
    @Transactional
    fun list(@QueryParam("userId") userId: String?): List<NotificationResponse> {
        log.info("Listing notifications for userId=$userId")

        val parsedUserId = parseUserId(userId)

        if (User.findById(parsedUserId) == null) {
            throw ApiErrors.badRequest("No user found")
        }

        return Notification.find("recipient.id = ?1", Sort.descending("createdAt"), parsedUserId)
            .list()
            .map { it.toResponse() }
    }

    @PATCH
    @Path("{id}/read")
    @Transactional
    fun markAsRead(@PathParam("id") id: String): NotificationResponse {
        val notificationId = parseNotificationId(id)

        val notification = Notification.findById(notificationId)
            ?: throw ApiErrors.notFound("Notification not found")

        notification.read = true

        log.info("Marked notification id=$notificationId as read")

        return notification.toResponse()
    }

    private fun parseUserId(userId: String?): UUID {
        if (userId.isNullOrBlank()) {
            throw ApiErrors.badRequest("Missing userId")
        }

        return try {
            UUID.fromString(userId)
        } catch (_: IllegalArgumentException) {
            throw ApiErrors.badRequest("Invalid userId")
        }
    }

    private fun parseNotificationId(id: String): UUID =
        try {
            UUID.fromString(id)
        } catch (_: IllegalArgumentException) {
            throw ApiErrors.notFound("Notification not found")
        }
}
