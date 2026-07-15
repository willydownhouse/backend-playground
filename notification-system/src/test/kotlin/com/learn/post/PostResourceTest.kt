package com.learn.post

import com.learn.notification.Notification
import com.learn.user.User
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import jakarta.transaction.Transactional
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.notNullValue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@QuarkusTest
class PostResourceTest {

    @BeforeEach
    @Transactional
    fun beforeEach() {
        Notification.deleteAll()
        Post.deleteAll()
        User.deleteAll()
    }

    @Test
    fun `create and list posts`() {
        val authorId = given()
            .contentType(ContentType.JSON)
            .body("""{"username":"alice","email":"alice@example.com"}""")
            .post("/users")
            .then()
            .statusCode(201)
            .extract()
            .path<String>("id")

        given()
            .contentType(ContentType.JSON)
            .body("""{"authorId":"$authorId","title":"Hello world","body":"My first post"}""")
            .post("/posts")
            .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("authorId", equalTo(authorId))
            .body("title", equalTo("Hello world"))
            .body("body", equalTo("My first post"))
            .body("createdAt", notNullValue())

        given()
            .get("/posts")
            .then()
            .statusCode(200)
            .body("size()", equalTo(1))
            .body("[0].title", equalTo("Hello world"))
            .body("[0].authorId", equalTo(authorId))
            .body("[0].body", equalTo("My first post"))
    }

    @Test
    fun `reject unknown author`() {
        given()
            .contentType(ContentType.JSON)
            .body("""{"authorId":"${UUID.randomUUID()}","title":"Hello","body":"Body"}""")
            .post("/posts")
            .then()
            .statusCode(400)
            .body("status", equalTo(400))
            .body("message", equalTo("Invalid authorId"))
    }

    @Test
    fun `reject blank title`() {
        val authorId = given()
            .contentType(ContentType.JSON)
            .body("""{"username":"alice","email":"alice@example.com"}""")
            .post("/users")
            .then()
            .statusCode(201)
            .extract()
            .path<String>("id")

        given()
            .contentType(ContentType.JSON)
            .body("""{"authorId":"$authorId","title":"","body":"Body"}""")
            .post("/posts")
            .then()
            .statusCode(400)
    }

    @Test
    fun `reject blank body`() {
        val authorId = given()
            .contentType(ContentType.JSON)
            .body("""{"username":"alice","email":"alice@example.com"}""")
            .post("/users")
            .then()
            .statusCode(201)
            .extract()
            .path<String>("id")

        given()
            .contentType(ContentType.JSON)
            .body("""{"authorId":"$authorId","title":"","body":""}""")
            .post("/posts")
            .then()
            .statusCode(400)
    }
}
