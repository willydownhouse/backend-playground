package com.learn.user

import io.quarkus.arc.profile.IfBuildProfile
import io.quarkus.runtime.StartupEvent
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import jakarta.transaction.Transactional
import org.jboss.logging.Logger

@IfBuildProfile("dev")
@ApplicationScoped
class DevDataSeeder {

    private val log = Logger.getLogger(DevDataSeeder::class.java)

    @Transactional
    fun onStart(@Observes event: StartupEvent) {
        if (User.count() > 0) {
            return
        }

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
}
