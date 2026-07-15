package com.learn.user

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import jakarta.transaction.Transactional
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.notNullValue
import org.jboss.logging.Logger
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@QuarkusTest
class UserResourceTest {

    private val log = Logger.getLogger(UserResourceTest::class.java)

    @BeforeAll
    @Transactional
    fun beforeAll() {
        log.info("Before all tests: user count = ${User.count()}")
    }

    @BeforeEach
    @Transactional
    fun beforeEach() {
        User.deleteAll()
        log.info("Before test: user count = ${User.count()}")
    }

    @Test
    fun `create and list users`() {
        val createdUser = given()
            .contentType(ContentType.JSON)
            .body("""{"username":"alice","email":"alice@example.com"}""")
            .post("/users")
            .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("username", equalTo("alice"))
            .body("email", equalTo("alice@example.com"))
            .extract()
            .response()
            .body
            .asString()

        log.info("Created user: $createdUser")

        val users = given()
            .get("/users")
            .then()
            .statusCode(200)
            .body("size()", equalTo(1))
            .body("[0].username", equalTo("alice"))
            .extract()
            .response()
            .body
            .asString()

        log.info("Listed users: $users")
    }

    @Test
    fun `reject blank email`() {
        given()
            .contentType(ContentType.JSON)
            .body("""{"username":"john","email":""}""")
            .post("/users")
            .then()
            .statusCode(400)
    }

    @Test
    fun `reject invalid email format`() {
        given()
            .contentType(ContentType.JSON)
            .body("""{"username":"john","email":"not-an-email"}""")
            .post("/users")
            .then()
            .statusCode(400)
    }

    @Test
    fun `reject blank username`() {
        given()
            .contentType(ContentType.JSON)
            .body("""{"username":"","email":"bob2@example.com"}""")
            .post("/users")
            .then()
            .statusCode(400)
    }

    @Test
    fun `reject duplicate username`() {
        val body = """{"username":"bob","email":"bob@example.com"}"""
        given().contentType(ContentType.JSON).body(body).post("/users").then().statusCode(201)

        given()
            .contentType(ContentType.JSON)
            .body("""{"username":"bob","email":"bob2@example.com"}""")
            .post("/users")
            .then()
            .statusCode(409)
            .body("status", equalTo(409))
            .body("message", equalTo("Username already exists"))
    }
}
