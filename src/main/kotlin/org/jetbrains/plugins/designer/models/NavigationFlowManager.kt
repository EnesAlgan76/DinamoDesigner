package org.jetbrains.plugins.designer.models

class NavigationFlowManager(private val screenManager: ScreenManager) {
    private val connections = mutableListOf<Connection>()

    fun addConnection(sourceScreenId: String, targetScreenId: String, componentId: String, label: String?) {
        if (!validateConnection(sourceScreenId, targetScreenId)) {
            throw IllegalArgumentException("Invalid connection: screens not found")
        }

        removeConnection(componentId)

        val connection = Connection(
            sourceScreenId = sourceScreenId,
            targetScreenId = targetScreenId,
            componentId = componentId,
            label = label
        )

        connections.add(connection)
    }

    fun removeConnection(componentId: String) {
        connections.removeIf { it.componentId == componentId }
    }

    fun getConnectionsForScreen(screenId: String): List<Connection> {
        return connections.filter { it.sourceScreenId == screenId }
    }

    fun getAllConnections(): List<Connection> {
        return connections.toList()
    }

    fun updateConnectionTarget(componentId: String, newTargetScreenId: String) {
        val connection = connections.find { it.componentId == componentId } ?: return

        if (!validateConnection(connection.sourceScreenId, newTargetScreenId)) {
            throw IllegalArgumentException("Invalid target screen")
        }

        connections.remove(connection)
        connections.add(
            connection.copy(targetScreenId = newTargetScreenId)
        )
    }

    fun validateConnection(sourceScreenId: String, targetScreenId: String): Boolean {
        val sourceExists = screenManager.getScreen(sourceScreenId) != null
        val targetExists = screenManager.getScreen(targetScreenId) != null

        if (!sourceExists || !targetExists) return false

        if (sourceScreenId == targetScreenId) return false

        return true
    }

    fun getConnectionByComponent(componentId: String): Connection? {
        return connections.find { it.componentId == componentId }
    }

    fun getTargetScreen(componentId: String): Screen? {
        val connection = connections.find { it.componentId == componentId } ?: return null
        return screenManager.getScreen(connection.targetScreenId)
    }

    fun getIncomingConnections(screenId: String): List<Connection> {
        return connections.filter { it.targetScreenId == screenId }
    }

    fun getOutgoingConnections(screenId: String): List<Connection> {
        return connections.filter { it.sourceScreenId == screenId }
    }

    fun hasCircularDependency(sourceScreenId: String, targetScreenId: String): Boolean {
        val visited = mutableSetOf<String>()
        return checkCircular(targetScreenId, sourceScreenId, visited)
    }

    private fun checkCircular(currentScreenId: String, targetScreenId: String, visited: MutableSet<String>): Boolean {
        if (currentScreenId == targetScreenId) return true
        if (visited.contains(currentScreenId)) return false

        visited.add(currentScreenId)

        val outgoing = getOutgoingConnections(currentScreenId)
        return outgoing.any { checkCircular(it.targetScreenId, targetScreenId, visited) }
    }

    fun clearAll() {
        connections.clear()
    }

    fun removeConnectionsForScreen(screenId: String) {
        connections.removeIf { it.sourceScreenId == screenId || it.targetScreenId == screenId }
    }
}
