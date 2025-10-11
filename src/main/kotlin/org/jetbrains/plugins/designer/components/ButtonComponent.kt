package org.jetbrains.plugins.template.designer.components

import org.jetbrains.plugins.designer.components.PropertyDescriptor
import org.jetbrains.plugins.designer.models.ComponentInstance
import org.jetbrains.plugins.designer.models.Screen
import javax.swing.ImageIcon

object ButtonComponent : ComponentDefinition {
    override val type = "BUTTON"
    override val displayName = "Button"

    override val propertyDescriptors = listOf(
        PropertyDescriptor.Text("identifier", "BUTTON"),
        PropertyDescriptor.Text("text", "Click Me"),
        PropertyDescriptor.Text("buttonType", "PRIMARY"),
        PropertyDescriptor.ScreenReference("targetScreen")
    )

    override fun getDisplayIcon(size: Int?): ImageIcon {
        return try {
            val resource = javaClass.getResource("/icons/button.png")
                ?: throw IllegalArgumentException("Icon not found")

            if (size != null) {
                val originalIcon = ImageIcon(resource)
                val scaledImage = originalIcon.image.getScaledInstance(size, size, java.awt.Image.SCALE_SMOOTH)
                ImageIcon(scaledImage)
            } else {
                ImageIcon(resource)
            }
        } catch (e: Exception) {
            ImageIcon()
        }
    }

    override fun generateCode(component: ComponentInstance, allScreens: List<Screen>): String {
        val props = component.properties
        val identifier = props["identifier"] as? String ?: "BUTTON"
        val varName = identifier.lowercase().replace("_", "")
        val text = props["text"] as? String ?: "Click Me"
        val targetScreenId = props["targetScreen"] as? String

        return buildString {
            appendLine("        TFComponentButton $varName = new TFComponentButton(\"$identifier\");")
            appendLine("        $varName.setText(messages.getMessage(\"${text.lowercase().replace(" ", "_")}\"));")

            if (!targetScreenId.isNullOrEmpty()) {
                val targetScreen = allScreens.find { it.id == targetScreenId }
                if (targetScreen != null) {
                    appendLine("        List<HashMap> ${varName}Actions = new ArrayList<HashMap>();")
                    appendLine("        HashMap ${varName}Action = new HashMap();")
                    appendLine("        ${varName}Action.put(TFIdentifier.ACTION, TFIdentifier.REDIRECT);")
                    appendLine("        ${varName}Action.put(TFIdentifier.ACTIONIDENTIFIER, ${targetScreen.name});")
                    appendLine("        ${varName}Actions.add(${varName}Action);")
                    appendLine("        $varName.setActions(${varName}Actions);")
                }
            }

            appendLine("        rowViewModelList.add($varName);")
        }
    }

    override fun validateProperties(properties: Map<String, Any>): Boolean {
        val identifier = properties["identifier"] as? String ?: return false
        val text = properties["text"] as? String ?: return false

        return identifier.isNotBlank() && text.isNotBlank()
    }
}
