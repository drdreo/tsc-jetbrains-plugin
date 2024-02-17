import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import java.net.URI

@Serializable
data class CodeSuggestion(
    val user: String,
    val code: String
)

@Service(Service.Level.PROJECT)
class MyWebSocketService(private val project: Project) : CoroutineScope by CoroutineScope(Dispatchers.Default) {

    private lateinit var websocketClientImpl: WebSocketClientImpl
    private val _codeSuggestion = MutableStateFlow<CodeSuggestion>(CodeSuggestion("none", "initial"))
    val codeSuggestion = _codeSuggestion.asStateFlow()

    init {
        initializeWebSocket()
        subscribeToEvents()
    }

    private fun initializeWebSocket() {
        try {
            println("initializing websocket client")
            val serverUri = URI("ws://localhost:8088/ws")
            websocketClientImpl = WebSocketClientImpl(serverUri).apply {
                connect()
            }
        } catch (e: Exception) {
            thisLogger().error("Error initializing WebSocket", e)
        }
    }

    private fun subscribeToEvents() {
        launch {
            println("subscribing to client messages")

            websocketClientImpl.messages
                .map { decodeMessage(it) }
                .collect { suggestion ->
                    _codeSuggestion.tryEmit(suggestion)
                }
        }
    }

    private fun decodeMessage(message: String): CodeSuggestion {
        println("Decoding WebSocket message: $message")
        return Json.decodeFromString<CodeSuggestion>(message)
    }

    private fun closeWebSocket() {
        websocketClientImpl.close()
    }

    // Implement service cleanup if needed, e.g., disconnect websocket on project close
}
