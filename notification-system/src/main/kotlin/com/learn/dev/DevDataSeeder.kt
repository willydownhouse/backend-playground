package com.learn.dev

import com.learn.notification.NotificationService
import com.learn.post.Post
import com.learn.user.User
import io.quarkus.arc.profile.IfBuildProfile
import io.quarkus.runtime.StartupEvent
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import jakarta.transaction.Transactional
import org.jboss.logging.Logger

@IfBuildProfile("dev")
@ApplicationScoped
class DevDataSeeder(
    private val notificationService: NotificationService,
) {

    private val log = Logger.getLogger(DevDataSeeder::class.java)

    @Transactional
    fun onStart(@Observes event: StartupEvent) {
        if (User.count() == 0L) {
            listOf(
                "alice" to "alice@example.com",
                "bob" to "bob@example.com",
            ).forEach { (username, email) ->
                User().apply {
                    this.username = username
                    this.email = email
                }.persist()
            }

            log.info("Seeded 2 dev users")
        }

//        if (Post.count() == 0L) {
//            val alice = User.find("username", "alice").firstResult()
//            if (alice == null) {
//                log.warn("Skipping post seed: alice not found")
//                return
//            }
//
//            val post = Post().apply {
//                author = alice
//                title = "Hello from Alice"
//                body = "Welcome to the notification system dev environment."
//            }
//            post.persistAndFlush()
//            notificationService.onPostCreated(post)
//
//            log.info("Seeded 1 dev post")
//        }
    }
}
