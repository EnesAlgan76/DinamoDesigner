package org.jetbrains.plugins.template.designer.components

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

    override fun generateCode(component: ComponentInstance, allScreens: List<Screen>): String {
        val props = component.properties
        val identifier = props["identifier"] as? String ?: "COMBOBOX"
        val varName = identifier.lowercase().replace("_", "")
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
            appendLine("        TFComponentComboBoxInput $varName = new TFComponentComboBoxInput(\"$identifier\");")
            appendLine("        $varName.setTitle(messages.getMessage(\"${title.lowercase().replace(" ", "_")}\"));")

            if (placeholder != "Please select") {
                appendLine("        $varName.setPlaceHolder(messages.getMessage(\"${placeholder.lowercase().replace(" ", "_")}\"));")
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
                appendLine("        $varName.setInformationString(messages.getMessage(\"${informationString.lowercase().replace(" ", "_")}\"));")
            }

            if (informationTitle.isNotEmpty()) {
                appendLine("        $varName.setInformationTitle(messages.getMessage(\"${informationTitle.lowercase().replace(" ", "_")}\"));")
            }

            if (required) {
                appendLine("        $varName.setValidation(true, messages.getMessage(\"${title.lowercase().replace(" ", "_")}_required_message\"));")
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
