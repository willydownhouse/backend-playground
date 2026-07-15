package com.learn

import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Path("/")
class HelloResource {

    @GET
    @Path("hello")
    @Produces(MediaType.TEXT_PLAIN)
    fun hello(): String = "Hello World"

    @GET
    @Path("info")
    @Produces(MediaType.APPLICATION_JSON)
    fun info(): HelloInfo = HelloInfo(
        message = "Hello from notification-system",
        service = "notification-system",
    )
}
