package org.jetbrains.plugins.designer.models

class ComponentManager {
    private var componentCounter = 0

    fun addComponentToScreen(screen: Screen, componentType: String, defaultProps: Map<String, Any>): ComponentInstance {
        componentCounter++

        val component = ComponentInstance(
            id = "component_$componentCounter",
            type = componentType,
            properties = defaultProps.toMutableMap()
        )

        screen.components.add(component)
        return component
    }

    fun removeComponent(screen: Screen, componentId: String): Boolean {
        val component = screen.components.find { it.id == componentId } ?: return false
        return screen.components.remove(component)
    }

    fun updateComponentProperty(componentId: String, key: String, value: Any, screen: Screen): Boolean {
        val component = screen.components.find { it.id == componentId } ?: return false
        component.properties[key] = value
        return true
    }

    fun getComponent(screen: Screen, componentId: String): ComponentInstance? {
        return screen.components.find { it.id == componentId }
    }

    fun getNextComponentId(): String {
        return "component_${componentCounter + 1}"
    }

    fun moveComponent(screen: Screen, componentId: String, newIndex: Int): Boolean {
        val component = screen.components.find { it.id == componentId } ?: return false

        if (newIndex < 0 || newIndex >= screen.components.size) return false

        screen.components.remove(component)
        screen.components.add(newIndex, component)
        return true
    }

    fun duplicateComponent(screen: Screen, componentId: String): ComponentInstance? {
        val original = screen.components.find { it.id == componentId } ?: return null

        componentCounter++
        val duplicate = ComponentInstance(
            id = "component_$componentCounter",
            type = original.type,
            properties = original.properties.toMutableMap()
        )

        // Update identifier property if exists
        if (duplicate.properties.containsKey("identifier")) {
            val originalId = duplicate.properties["identifier"] as? String ?: ""
            duplicate.properties["identifier"] = "${originalId}_COPY"
        }

        screen.components.add(duplicate)
        return duplicate
    }

    fun getComponentCount(screen: Screen): Int = screen.components.size

    fun getComponentsByType(screen: Screen, type: String): List<ComponentInstance> {
        return screen.components.filter { it.type == type }
    }

    fun resetCounter() {
        componentCounter = 0
    }

    fun setCounterIfHigher(value: Int) {
        if (value > componentCounter) {
            componentCounter = value
        }
    }
}
