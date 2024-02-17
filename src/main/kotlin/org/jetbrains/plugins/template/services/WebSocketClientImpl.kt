import com.intellij.openapi.diagnostic.Logger
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI

class WebSocketClientImpl(serverUri: URI?) : WebSocketClient(serverUri) {
    private val logger = Logger.getInstance(WebSocketClient::class.java)
    private val _messages = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = Int.MAX_VALUE)
    val messages = _messages.asSharedFlow()

    override fun onOpen(handshakedata: ServerHandshake) {
        println("WebSocket Connected")
    }

    override fun onMessage(message: String) {
        println("Received: $message")
        val sent = _messages.tryEmit(message)
        if(!sent){
            logger.warn("message not sent")
        }
    }

    override fun onClose(code: Int, reason: String, remote: Boolean) {
        println("WebSocket Closed: $reason ($code)")
    }

    override fun onError(ex: Exception) {
        println("WebSocket Error" + ex)
    }
}

