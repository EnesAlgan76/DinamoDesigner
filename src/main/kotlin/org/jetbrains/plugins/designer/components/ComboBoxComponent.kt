package org.jetbrains.plugins.template.designer.components

import com.intellij.openapi.project.Project
import org.jetbrains.plugins.designer.codegen.TFIdentifierManager
import org.jetbrains.plugins.designer.codegen.TFMessagesManager
import org.jetbrains.plugins.designer.components.PropertyDescriptor
import org.jetbrains.plugins.designer.models.ComponentInstance
import org.jetbrains.plugins.designer.models.Screen
import javax.swing.ImageIcon

object ComboBoxComponent : ComponentDefinition {
    override val type = "COMBO_BOX"
    override val displayName = "ComboBox"

    override val propertyDescriptors = listOf(
        PropertyDescriptor.Text("identifier", "COMBOBOX"),
        PropertyDescriptor.Text("title", "Select option"),
        PropertyDescriptor.Text("items", "Option 1,Option 2,Option 3"),
        PropertyDescriptor.Number("selectedIndex", 0),
        PropertyDescriptor.Text("placeholder", "Please select"),
        PropertyDescriptor.Boolean("showPlaceholderAsFirstItem", false),
        PropertyDescriptor.Boolean("required", false),
        PropertyDescriptor.Text("informationString", ""),
        PropertyDescriptor.Text("informationTitle", "")
    )

    override fun getDisplayIcon(size: Int?): ImageIcon {
        return try {
            val resource = javaClass.getResource("/icons/combobox.png")
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
        val identifierValue = props["identifier"] as? String ?: "COMBOBOX"
        val identifier = TFIdentifierManager.getOrCreateIdentifier(project, identifierValue)
        val varName = identifierValue.lowercase().replace("_", "")
        val title = props["title"] as? String ?: "Select option"
        val itemsStr = props["items"] as? String ?: ""
        val items = itemsStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val selectedIndex = props["selectedIndex"] as? Int ?: -1
        val placeholder = props["placeholder"] as? String ?: "Please select"
        val showPlaceholderAsFirstItem = props["showPlaceholderAsFirstItem"] as? Boolean ?: false
        val required = props["required"] as? Boolean ?: false
        val informationString = props["informationString"] as? String ?: ""
        val informationTitle = props["informationTitle"] as? String ?: ""

        return buildString {
            appendLine("        TFComponentComboBoxInput $varName = new TFComponentComboBoxInput($identifier);")
            val titleMessage = TFMessagesManager.getOrCreateMessage(project, title)
            appendLine("        $varName.setTitle($titleMessage);")

            if (placeholder != "Please select") {
                val placeholderMessage = TFMessagesManager.getOrCreateMessage(project, placeholder)
                appendLine("        $varName.setPlaceHolder($placeholderMessage);")
            }

            if (items.isNotEmpty()) {
                appendLine("        List<String> ${varName}Items = Arrays.asList(${items.joinToString(", ") { "\"$it\"" }});")
                appendLine("        $varName.setItems(${varName}Items);")
            }

            if (selectedIndex >= 0) {
                appendLine("        $varName.setSelectedIndex($selectedIndex);")
            }

            if (showPlaceholderAsFirstItem) {
                appendLine("        $varName.setShowPlaceholderAsFirstItem(true);")
            }

            if (informationString.isNotEmpty()) {
                val infoStringMessage = TFMessagesManager.getOrCreateMessage(project, informationString)
                appendLine("        $varName.setInformationString($infoStringMessage);")
            }

            if (informationTitle.isNotEmpty()) {
                val infoTitleMessage = TFMessagesManager.getOrCreateMessage(project, informationTitle)
                appendLine("        $varName.setInformationTitle($infoTitleMessage);")
            }

            if (required) {
                val requiredMessage = TFMessagesManager.getOrCreateMessage(project, "${title} is required")
                appendLine("        $varName.setValidation(true, $requiredMessage);")
            }

            appendLine("        rowViewModelList.add($varName);")
        }
    }

    override fun validateProperties(properties: Map<String, Any>): Boolean {
        val identifier = properties["identifier"] as? String ?: return false
        val items = properties["items"] as? String ?: return false

        return identifier.isNotBlank() && items.isNotBlank()
    }
}
