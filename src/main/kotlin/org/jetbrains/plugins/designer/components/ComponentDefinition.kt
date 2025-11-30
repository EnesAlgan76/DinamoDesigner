package org.jetbrains.plugins.template.designer.components

import com.intellij.openapi.project.Project
import org.jetbrains.plugins.designer.components.PropertyDescriptor
import org.jetbrains.plugins.designer.models.ComponentInstance
import org.jetbrains.plugins.designer.models.Screen
import javax.swing.ImageIcon

interface ComponentDefinition {
    val type: String
    val displayName: String
    val propertyDescriptors: List<PropertyDescriptor>

    fun getDisplayIcon(size: Int?): ImageIcon
    fun generateCode(project: Project, component: ComponentInstance, allScreens: List<Screen>): String
    fun validateProperties(properties: Map<String, Any>): Boolean

    fun createDefaultProperties(counter: Int): Map<String, Any> {
        val properties = mutableMapOf<String, Any>()

        propertyDescriptors.forEach { descriptor ->
            when (descriptor) {
                is PropertyDescriptor.Text -> {
                    val value = if (descriptor.key == "identifier") {
                        // Capitalize first letter and append counter
                        val baseIdentifier = descriptor.default
                        val capitalized = baseIdentifier.toString().replaceFirstChar { it.uppercase() }
                        "$capitalized$counter"
                    } else {
                        descriptor.default
                    }
                    properties[descriptor.key] = value
                }
                is PropertyDescriptor.Number -> properties[descriptor.key] = descriptor.default
                is PropertyDescriptor.Boolean -> properties[descriptor.key] = descriptor.default
                is PropertyDescriptor.Enum -> properties[descriptor.key] = descriptor.default
                is PropertyDescriptor.ScreenReference -> properties[descriptor.key] = ""
                is PropertyDescriptor.ConditionalGroup -> {
                    // Add toggle key
                    properties[descriptor.toggleKey] = descriptor.default

                    // Add all child properties with their defaults
                    descriptor.childProperties.forEach { childDesc ->
                        when (childDesc) {
                            is PropertyDescriptor.Text -> properties[childDesc.key] = childDesc.default
                            is PropertyDescriptor.Number -> properties[childDesc.key] = childDesc.default
                            is PropertyDescriptor.Boolean -> properties[childDesc.key] = childDesc.default
                            is PropertyDescriptor.Enum -> properties[childDesc.key] = childDesc.default
                            is PropertyDescriptor.ScreenReference -> properties[childDesc.key] = ""
                            else -> {}
                        }
                    }
                }
            }
        }

        return properties
    }
}
