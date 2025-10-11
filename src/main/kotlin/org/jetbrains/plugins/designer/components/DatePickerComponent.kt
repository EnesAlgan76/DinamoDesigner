package org.jetbrains.plugins.template.designer.components

import com.intellij.openapi.project.Project
import org.jetbrains.plugins.designer.codegen.TFIdentifierManager
import org.jetbrains.plugins.designer.components.PropertyDescriptor
import org.jetbrains.plugins.designer.models.ComponentInstance
import org.jetbrains.plugins.designer.models.Screen
import javax.swing.ImageIcon

object DatePickerComponent : ComponentDefinition {
    override val type = "DATE_PICKER"
    override val displayName = "Date Picker"

    override val propertyDescriptors = listOf(
        PropertyDescriptor.Text("identifier", "DATEPICKER"),
        PropertyDescriptor.Text("title", "Select date"),
        PropertyDescriptor.Boolean("validation", true),
        PropertyDescriptor.Text("minDate", "today"),
        PropertyDescriptor.Text("maxDate", "")
    )

    override fun getDisplayIcon(size: Int?): ImageIcon {
        return try {
            val resource = javaClass.getResource("/icons/datepicker.png")
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

    override fun generateCode(project: Project, component: ComponentInstance, allScreens: List<Screen>): String {
        val props = component.properties
        val identifierValue = props["identifier"] as? String ?: "DATEPICKER"
        val identifier = TFIdentifierManager.getOrCreateIdentifier(project, identifierValue)
        val varName = identifierValue.lowercase().replace("_", "")
        val title = props["title"] as? String ?: "Select date"
        val validation = props["validation"] as? Boolean ?: true

        return buildString {
            appendLine("        TFComponentDateInput $varName = new TFComponentDateInput($identifier);")
            appendLine("        $varName.setTitle(messages.getMessage(\"${title.lowercase().replace(" ", "_")}\"));")
            if (validation) {
                appendLine("        $varName.setValidation(true);")
            }
            appendLine("        rowViewModelList.add($varName);")
        }
    }

    override fun validateProperties(properties: Map<String, Any>): Boolean {
        val identifier = properties["identifier"] as? String ?: return false
        return identifier.isNotBlank()
    }
}
