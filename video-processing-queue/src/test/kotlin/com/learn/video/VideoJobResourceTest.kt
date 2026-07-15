package com.learn.video

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import jakarta.transaction.Transactional
import org.hamcrest.CoreMatchers.equalTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@QuarkusTest
class VideoJobResourceTest {

    @BeforeEach
    @Transactional
    fun beforeEach() {
        VideoJob.deleteAll()
    }

    @Test
    fun createPersistsJobWithQueuedStatus() {
        given()
            .contentType(ContentType.JSON)
            .body("""{"inputUrl":"https://example.com/video.mov"}""")
            .`when`()
            .post("/videos/jobs")
            .then()
            .statusCode(201)
            .body("inputUrl", equalTo("https://example.com/video.mov"))
            .body("status", equalTo("QUEUED"))
            .body("attempts", equalTo(0))
            .body("outputUrl", equalTo(null))
            .body("errorMessage", equalTo(null))
    }

    @Test
    fun rejectInvalidUrl() {
        given()
            .contentType(ContentType.JSON)
            .body("""{"inputUrl":"not-a-url"}""")
            .`when`()
            .post("/videos/jobs")
            .then()
            .statusCode(400)
    }

    @Test
    fun rejectBlankUrl() {
        given()
            .contentType(ContentType.JSON)
            .body("""{"inputUrl":""}""")
            .`when`()
            .post("/videos/jobs")
            .then()
            .statusCode(400)
    }

    @Test
    fun listReturnsAllJobs() {
        given()
            .contentType(ContentType.JSON)
            .body("""{"inputUrl":"https://example.com/first.mov"}""")
            .post("/videos/jobs")
            .then()
            .statusCode(201)

        given()
            .contentType(ContentType.JSON)
            .body("""{"inputUrl":"https://example.com/second.mov"}""")
            .post("/videos/jobs")
            .then()
            .statusCode(201)

        given()
            .`when`()
            .get("/videos/jobs")
            .then()
            .statusCode(200)
            .body("size()", equalTo(2))
            .body("[0].inputUrl", equalTo("https://example.com/second.mov"))
            .body("[1].inputUrl", equalTo("https://example.com/first.mov"))
    }

    @Test
    fun deleteAllRemovesEveryJob() {
        given()
            .contentType(ContentType.JSON)
            .body("""{"inputUrl":"https://example.com/first.mov"}""")
            .post("/videos/jobs")
            .then()
            .statusCode(201)

        given()
            .contentType(ContentType.JSON)
            .body("""{"inputUrl":"https://example.com/second.mov"}""")
            .post("/videos/jobs")
            .then()
            .statusCode(201)

        given()
            .`when`()
            .delete("/videos/jobs")
            .then()
            .statusCode(204)

        given()
            .`when`()
            .get("/videos/jobs")
            .then()
            .statusCode(200)
            .body("size()", equalTo(0))
    }
}
