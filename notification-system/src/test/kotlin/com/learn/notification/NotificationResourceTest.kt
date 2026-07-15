package com.learn.notification

import com.learn.user.User
import io.quarkus.narayana.jta.QuarkusTransaction
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import jakarta.transaction.Transactional
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@QuarkusTest
class NotificationResourceTest {

    @BeforeEach
    @Transactional
    fun beforeEach() {
        Notification.deleteAll()
        User.deleteAll()
    }

    @Test
    fun `list notifications for user returns only their inbox`() {
        val aliceId = createUser("alice", "alice@example.com")
        val bobId = createUser("bob", "bob@example.com")
        val carolId = createUser("carol", "carol@example.com")

        seedNotifications {
            seedNewPostNotification(recipientId = bobId, actorId = aliceId, postTitle = "Hello world")
            seedNewPostNotification(recipientId = carolId, actorId = aliceId, postTitle = "Hello world")
        }

        given()
            .queryParam("userId", bobId)
            .get("/notifications")
            .then()
            .statusCode(200)
            .body("size()", equalTo(1))
            .body("[0].recipientId", equalTo(bobId))
            .body("[0].actorId", equalTo(aliceId))
            .body("[0].type", equalTo("NEW_POST"))
            .body("[0].title", equalTo("alice published a new post"))
            .body("[0].body", equalTo("Hello world"))
            .body("[0].read", equalTo(false))

        given()
            .queryParam("userId", aliceId)
            .get("/notifications")
            .then()
            .statusCode(200)
            .body("size()", equalTo(0))

        given()
            .queryParam("userId", carolId)
            .get("/notifications")
            .then()
            .statusCode(200)
            .body("size()", equalTo(1))
    }

    @Test
    fun `reject missing userId when listing notifications`() {
        given()
            .get("/notifications")
            .then()
            .statusCode(400)
            .body("status", equalTo(400))
            .body("message", equalTo("Missing userId"))
    }

    @Test
    fun `reject unknown userId when listing notifications`() {
        given()
            .queryParam("userId", UUID.randomUUID())
            .get("/notifications")
            .then()
            .statusCode(400)
            .body("status", equalTo(400))
            .body("message", equalTo("No user found"))
    }

    @Test
    fun `mark notification as read`() {
        val aliceId = createUser("alice", "alice@example.com")
        val bobId = createUser("bob", "bob@example.com")

        seedNotifications {
            seedNewPostNotification(recipientId = bobId, actorId = aliceId, postTitle = "Hello world")
        }

        val notificationId = given()
            .queryParam("userId", bobId)
            .get("/notifications")
            .then()
            .statusCode(200)
            .body("size()", equalTo(1))
            .extract()
            .path<String>("[0].id")

        given()
            .patch("/notifications/$notificationId/read")
            .then()
            .statusCode(200)
            .body("id", equalTo(notificationId))
            .body("recipientId", equalTo(bobId))
            .body("read", equalTo(true))

        given()
            .queryParam("userId", bobId)
            .get("/notifications")
            .then()
            .statusCode(200)
            .body("[0].read", equalTo(true))
    }

    @Test
    fun `mark notification as read is idempotent`() {
        val aliceId = createUser("alice", "alice@example.com")
        val bobId = createUser("bob", "bob@example.com")

        seedNotifications {
            seedNewPostNotification(recipientId = bobId, actorId = aliceId, postTitle = "Hello world")
        }

        val notificationId = given()
            .queryParam("userId", bobId)
            .get("/notifications")
            .then()
            .statusCode(200)
            .extract()
            .path<String>("[0].id")

        given().patch("/notifications/$notificationId/read").then().statusCode(200)
        given()
            .patch("/notifications/$notificationId/read")
            .then()
            .statusCode(200)
            .body("read", equalTo(true))
    }

    @Test
    fun `reject unknown notification when marking as read`() {
        given()
            .patch("/notifications/${UUID.randomUUID()}/read")
            .then()
            .statusCode(404)
            .body("status", equalTo(404))
            .body("message", equalTo("Notification not found"))
    }

    private fun createUser(username: String, email: String): String =
        given()
            .contentType(ContentType.JSON)
            .body("""{"username":"$username","email":"$email"}""")
            .post("/users")
            .then()
            .statusCode(201)
            .extract()
            .path("id")

    private fun seedNotifications(block: () -> Unit) {
        QuarkusTransaction.requiringNew().run(block)
    }

    private fun seedNewPostNotification(
        recipientId: String,
        actorId: String,
        postTitle: String,
        referenceId: UUID = UUID.randomUUID(),
    ) {
        val recipient = User.findById(UUID.fromString(recipientId))
            ?: error("Recipient not found: $recipientId")
        val actor = User.findById(UUID.fromString(actorId))
            ?: error("Actor not found: $actorId")

        Notification().apply {
            this.recipient = recipient
            this.actor = actor
            type = NotificationType.NEW_POST
            title = "${actor.username} published a new post"
            body = postTitle
            this.referenceId = referenceId
        }.persist()
    }
}
