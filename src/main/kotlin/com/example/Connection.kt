import io.ktor.websocket.*
import java.util.concurrent.atomic.AtomicInteger

class Connection(val session : DefaultWebSocketSession ,  userName: String) {
    companion object {
        val lastId = AtomicInteger(0)
    }
    val name = userName
}