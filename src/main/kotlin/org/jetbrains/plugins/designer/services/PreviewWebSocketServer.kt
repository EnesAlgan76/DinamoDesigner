package org.jetbrains.plugins.designer.services

import com.google.gson.Gson
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import org.jetbrains.plugins.designer.models.Screen
import java.net.InetSocketAddress

class PreviewWebSocketServer(port: Int = 8080) : WebSocketServer(InetSocketAddress(port)) {

    private val gson = Gson()

    override fun onOpen(conn: WebSocket, handshake: ClientHandshake) {
        val message = mapOf("type" to "connection", "message" to "Connected")
        conn.send(gson.toJson(message))
    }

    override fun onClose(conn: WebSocket, code: Int, reason: String, remote: Boolean) {}

    override fun onMessage(conn: WebSocket, message: String) {
        conn.send(gson.toJson(mapOf("type" to "echo", "data" to message)))
    }

    override fun onError(conn: WebSocket?, ex: Exception) {
        ex.printStackTrace()
    }

    override fun onStart() {}

    fun sendPreview(screen: Screen) {
        val payload = mapOf(
            "type" to "preview",
            "screen" to mapOf(
                "screenType" to screen.type.name,
                "identifier" to screen.name,
                "title" to screen.name,
                "inputs" to screen.components.map { component ->
                    mapOf(
                        "type" to component.type,
                        "identifier" to component.properties["identifier"],
                        "title" to component.properties["title"],
                        "placeholder" to component.properties["placeholder"],
                        "required" to component.properties["required"]
                    )
                }
            )
        )
        broadcast(gson.toJson(payload))
    }

    fun getStatus(): Map<String, Any> {
             return mapOf(
                     "port" to address.port,
                     "connections" to connections.size
                 )
         }

    companion object {
        private var instance: PreviewWebSocketServer? = null

        fun start(port: Int = 8080): PreviewWebSocketServer {
            if (instance == null) {
                instance = PreviewWebSocketServer(port)
                instance?.start()
            }
            return instance!!
        }

        fun stop() {
            instance?.stop()
            instance = null
        }
    }
}