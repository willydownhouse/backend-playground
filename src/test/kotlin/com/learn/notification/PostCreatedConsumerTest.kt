package com.learn.notification

import com.learn.post.PostCreatedEvent
import com.learn.user.User
import io.quarkus.narayana.jta.QuarkusTransaction
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.Instant
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@QuarkusTest
class PostCreatedConsumerTest {

    @Inject
    lateinit var consumer: PostCreatedConsumer

    @BeforeEach
    @Transactional
    fun beforeEach() {
        Notification.deleteAll()
        User.deleteAll()
    }

    @Test
    fun `creates notifications for other users but not author`() {
        val alice = persistUser("alice", "alice@example.com")
        persistUser("bob", "bob@example.com")
        persistUser("carol", "carol@example.com")
        val postId = UUID.randomUUID()

        consume(
            PostCreatedEvent(
                postId = postId,
                authorId = alice.id,
                authorUsername = alice.username,
                title = "Hello world",
                createdAt = Instant.now(),
            ),
        )

        inTransaction {
            assertEquals(2, Notification.count())

            val bobNotification = Notification.find("recipient.username", "bob").firstResult()!!
            assertEquals(NotificationType.NEW_POST, bobNotification.type)
            assertEquals("alice published a new post", bobNotification.title)
            assertEquals("Hello world", bobNotification.body)
            assertEquals(postId, bobNotification.referenceId)
            assertEquals(alice.id, bobNotification.actor.id)
            assertFalse(bobNotification.read)

            assertEquals(1, Notification.count("recipient.username", "carol"))
            assertEquals(0, Notification.count("recipient.id", alice.id))
        }
    }

    @Test
    fun `skips fan-out when author not found`() {
        consume(
            PostCreatedEvent(
                postId = UUID.randomUUID(),
                authorId = UUID.randomUUID(),
                authorUsername = "ghost",
                title = "Hello world",
                createdAt = Instant.now(),
            ),
        )

        inTransaction {
            assertEquals(0, Notification.count())
        }
    }

    private fun persistUser(username: String, email: String): User {
        lateinit var user: User
        QuarkusTransaction.requiringNew().run {
            user = User().apply {
                this.username = username
                this.email = email
            }
            user.persistAndFlush()
        }
        return user
    }

    private fun consume(event: PostCreatedEvent) {
        QuarkusTransaction.requiringNew().run {
            consumer.consume(event)
        }
    }

    private fun inTransaction(block: () -> Unit) {
        QuarkusTransaction.requiringNew().run(block)
    }
}
