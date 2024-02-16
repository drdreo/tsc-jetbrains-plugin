package org.jetbrains.plugins.template.services

import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI


class MyWebSocketClient(serverUri: URI?, private val onMessageReceived: (String) -> Unit) : WebSocketClient(serverUri) {

    override fun onOpen(handshakedata: ServerHandshake) {
        println("WebSocket Connected")
    }

    override fun onMessage(message: String) {
        println("Received: $message")
        onMessageReceived(message)
    }

    override fun onClose(code: Int, reason: String, remote: Boolean) {
        println("WebSocket Closed: $reason ($code)")
    }

    override fun onError(ex: Exception) {
        ex.printStackTrace()
    }
}
