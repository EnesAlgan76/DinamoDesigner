package org.jetbrains.plugins.template.designer.components

import org.jetbrains.plugins.designer.components.PropertyDescriptor
import org.jetbrains.plugins.designer.models.ComponentInstance
import org.jetbrains.plugins.designer.models.Screen
import javax.swing.ImageIcon

interface ComponentDefinition {
    val type: String
    val displayName: String
    val propertyDescriptors: List<PropertyDescriptor>

    fun getDisplayIcon(size: Int?): ImageIcon
    fun generateCode(component: ComponentInstance, allScreens: List<Screen>): String
    fun validateProperties(properties: Map<String, Any>): Boolean

    fun createDefaultProperties(counter: Int): Map<String, Any> {
        return propertyDescriptors.associate { descriptor ->
            val value = when (descriptor) {
                is PropertyDescriptor.Text -> {
                    if (descriptor.key == "identifier") {
                        "${descriptor.default}_$counter"
                    } else {
                        descriptor.default
                    }
                }
                is PropertyDescriptor.Number -> descriptor.default
                is PropertyDescriptor.Boolean -> descriptor.default
                is PropertyDescriptor.Enum -> descriptor.default
                is PropertyDescriptor.ScreenReference -> ""
            }
            descriptor.key to value
        }
    }
}
