package org.jetbrains.plugins.designer.services

import com.google.gson.Gson
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import org.jetbrains.plugins.designer.models.Screen
import java.net.InetSocketAddress
import kotlin.collections.set

class PreviewWebSocketServer(host: String = "10.141.8.82", port: Int = 8887) : WebSocketServer(InetSocketAddress(host, port)) {

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
                    component.properties.toMutableMap().apply {
                        put("inputType", component.type)

                        if (component.type == "ComboBoxInput" && containsKey("items")) {
                            val itemsStr = get("items") as? String ?: ""
                            if (itemsStr.isNotEmpty()) {
                                val itemsList = itemsStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                                put("items", itemsList)
                            }
                        }
                    }
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

        fun start(host: String = "10.0.2.2", port: Int = 8887): PreviewWebSocketServer {
            if (instance == null) {
                instance = PreviewWebSocketServer(host, port)
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