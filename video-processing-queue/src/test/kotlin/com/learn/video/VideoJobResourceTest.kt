package com.learn.video

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import org.hamcrest.CoreMatchers.equalTo
import org.junit.jupiter.api.Test

@QuarkusTest
class VideoJobResourceTest {

    @Test
    fun createReturnsSubmittedUrl() {
        given()
            .contentType(ContentType.JSON)
            .body("""{"inputUrl":"https://example.com/video.mov"}""")
            .`when`()
            .post("/videos/jobs")
            .then()
            .statusCode(201)
            .body("inputUrl", equalTo("https://example.com/video.mov"))
    }
}
