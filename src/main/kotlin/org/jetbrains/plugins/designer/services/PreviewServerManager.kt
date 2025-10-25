package org.jetbrains.plugins.designer.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.designer.models.Screen

@Service(Service.Level.PROJECT)
class PreviewServerManager(private val project: Project) {

    private var server: PreviewWebSocketServer? = null

    fun startServer(port: Int = 8080): Boolean {
        return try {
            if (server != null) return true

            server = PreviewWebSocketServer.start(port)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun stopServer() {
        PreviewWebSocketServer.stop()
        server = null
    }

    fun sendPreview(screen: Screen) {
        server?.sendPreview(screen)
    }

    fun isServerRunning(): Boolean = server != null

    fun getStatus(): String {
            return if (server != null) {
                    val status = server!!.getStatus()
                    status.toString()
                } else {
                    "🔴 Server Status: STOPPED"
                }
        }

    companion object {
        fun getInstance(project: Project): PreviewServerManager {
            return project.getService(PreviewServerManager::class.java)
        }
    }
}