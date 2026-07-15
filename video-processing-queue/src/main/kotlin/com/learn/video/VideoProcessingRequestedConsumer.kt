package com.learn.video

import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.reactive.messaging.Incoming
import org.jboss.logging.Logger

@ApplicationScoped
class VideoProcessingRequestedConsumer {

    private val log = Logger.getLogger(VideoProcessingRequestedConsumer::class.java)

    @Incoming("video-processing-requested-in")
    fun consume(event: VideoProcessingRequestedEvent) {
        log.info(
            "Received VideoProcessingRequestedEvent jobId=${event.jobId} inputUrl=${event.inputUrl} createdAt=${event.createdAt}",
        )
    }
}
