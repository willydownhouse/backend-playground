package com.learn.notification

import io.quarkus.narayana.jta.QuarkusTransaction
import jakarta.enterprise.context.ApplicationScoped
import org.hibernate.exception.ConstraintViolationException
import org.jboss.logging.Logger

@ApplicationScoped
class NotificationPersister {

    private val log = Logger.getLogger(NotificationPersister::class.java)

    /**
     * Inserts one notification in its own transaction so a duplicate-key failure
     * does not abort fan-out for the remaining recipients.
     */
    fun persistIfAbsent(notification: Notification) {
        try {
            QuarkusTransaction.requiringNew().run {
                notification.persist()
            }
        } catch (e: Exception) {
            if (!isDuplicateNotification(e)) {
                throw e
            }

            log.info(
                "Notification already exists recipientId=${notification.recipient.id} " +
                    "referenceId=${notification.referenceId} type=${notification.type}, skipping",
            )
        }
    }

    private fun isDuplicateNotification(e: Throwable): Boolean {
        var cause: Throwable? = e
        while (cause != null) {
            if (cause is ConstraintViolationException) {
                return cause.sqlException?.sqlState == "23505"
            }
            cause = cause.cause
        }
        return false
    }
}
