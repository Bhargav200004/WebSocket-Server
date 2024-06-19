package com.example.plugins

import Connection

import io.ktor.serialization.kotlinx.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import java.time.Duration
import java.util.*

fun Application.configureSockets() {
    install(WebSockets) {
        contentConverter = KotlinxWebsocketSerializationConverter(Json)
        pingPeriod = Duration.ofSeconds(1000)
        timeout = Duration.ofSeconds(1000)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
    routing {
        val connections = Collections.synchronizedSet<Connection?>(LinkedHashSet())

        webSocket("/chat/{name}") { // websocketSession
            val name = call.parameters["name"] ?: return@webSocket

            //If Connection also exist then return
            val existingConnection = connections.any { it?.name == name }
            if (existingConnection) {
                send("Already exists")
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Name already taken"))
                return@webSocket
            }

    
            println("Adding user!")
            val thisConnection = Connection(this, name)
            connections += thisConnection
            try {
                send("There are ${connections.size} users here!")
                send(connections.toString())
                for (frame in incoming) {
                    frame as? Frame.Text ?: continue
                    val receivedText = frame.readText()
                    val textWithUsername = "${thisConnection.name} : $receivedText"

                    connections.forEach {
                        if (receivedText.equals("bye", ignoreCase = true)) {
                            it.session.send("User are disconnected")
                            println("Removing $thisConnection!")
                            connections -= thisConnection
                            delay(1000)
                            close(CloseReason(CloseReason.Codes.NORMAL, "Client said BYE"))
                        } else {
                            it.session.send(textWithUsername)
                            println("${connections.size}" + textWithUsername)
                        }
                    }
                }
            } catch (e: Exception) {
                println(e.localizedMessage)
            } finally {
                println("Removing $thisConnection!")
                delay(1000)
                close(CloseReason(CloseReason.Codes.NORMAL, "Client said BYE"))
                connections -= thisConnection
            }
        }
    }
}


//                    if (frame is Frame.Text) {
//                        val text = frame.readText()
//                        outgoing.send(Frame.Text("YOU SAID: $text"))
//                        if (text.equals("bye", ignoreCase = true)) {
//                            close(CloseReason(CloseReason.Codes.NORMAL, "Client said BYE"))
//                        }
//                    }
