package org.jetbrains.plugins.template.designer.components

import org.jetbrains.plugins.designer.components.PropertyDescriptor
import org.jetbrains.plugins.designer.models.ComponentInstance
import org.jetbrains.plugins.designer.models.Screen
import javax.swing.ImageIcon

object CheckBoxComponent : ComponentDefinition {
    override val type = "CHECKBOX"
    override val displayName = "CheckBox"

    override val propertyDescriptors = listOf(
        PropertyDescriptor.Text("identifier", "CHECKBOX"),
        PropertyDescriptor.Text("text", "Checkbox Text"),
        PropertyDescriptor.Text("underlineText", ""),
        PropertyDescriptor.Text("descriptionText", ""),
        PropertyDescriptor.Boolean("checked", false),
        PropertyDescriptor.Boolean("required", false),
        PropertyDescriptor.Boolean("showPopUp", false),
        PropertyDescriptor.Text("popUpTitle", ""),
        PropertyDescriptor.Text("popUpText", ""),
        PropertyDescriptor.Text("continueButton", "Continue"),
        PropertyDescriptor.Text("cancelButton", "Cancel"),
        PropertyDescriptor.Text("informationString", ""),
        PropertyDescriptor.Text("informationAlertTitle", "")
    )

    override fun getDisplayIcon(size: Int?): ImageIcon {
        return try {
            val resource = javaClass.getResource("/icons/checkbox.png")
                ?: throw IllegalArgumentException("Icon not found: /icons/checkbox.png")

            if (size != null) {
                val originalIcon = ImageIcon(resource)
                val scaledImage = originalIcon.image.getScaledInstance(size, size, java.awt.Image.SCALE_SMOOTH)
                ImageIcon(scaledImage)
            } else {
                ImageIcon(resource)
            }
        } catch (e: Exception) {
            ImageIcon() // Empty icon as fallback
        }
    }

    override fun generateCode(component: ComponentInstance, allScreens: List<Screen>): String {
        val props = component.properties
        val identifier = props["identifier"] as? String ?: "CHECKBOX"
        val varName = identifier.lowercase().replace("_", "")
        val text = props["text"] as? String ?: "Checkbox Text"
        val underlineText = props["underlineText"] as? String ?: ""
        val descriptionText = props["descriptionText"] as? String ?: ""
        val checked = props["checked"] as? Boolean ?: false
        val showPopUp = props["showPopUp"] as? Boolean ?: false
        val popUpTitle = props["popUpTitle"] as? String ?: ""
        val popUpText = props["popUpText"] as? String ?: ""
        val continueButton = props["continueButton"] as? String ?: "Continue"
        val cancelButton = props["cancelButton"] as? String ?: "Cancel"
        val informationString = props["informationString"] as? String ?: ""
        val informationAlertTitle = props["informationAlertTitle"] as? String ?: ""

        return buildString {
            appendLine("        TFComponentCheckBox $varName = new TFComponentCheckBox(\"$identifier\");")
            appendLine("        $varName.setText(messages.getMessage(\"${text.lowercase().replace(" ", "_")}\"));")

            if (underlineText.isNotEmpty()) {
                appendLine("        $varName.setUnderlineText(messages.getMessage(\"${underlineText.lowercase().replace(" ", "_")}\"));")
            }

            if (descriptionText.isNotEmpty()) {
                appendLine("        $varName.setDescriptionText(messages.getMessage(\"${descriptionText.lowercase().replace(" ", "_")}\"));")
            }

            appendLine("        $varName.setChecked($checked);")

            if (showPopUp) {
                appendLine("        $varName.setShowPopUp(true);")
                appendLine("        $varName.setPopUpTitle(messages.getMessage(\"${popUpTitle.lowercase().replace(" ", "_")}\"));")
                appendLine("        $varName.setInformationMessage(messages.getMessage(\"${popUpText.lowercase().replace(" ", "_")}\"));")
                appendLine("        $varName.setContinueButton(messages.getMessage(\"${continueButton.lowercase().replace(" ", "_")}\"));")
                appendLine("        $varName.setCancelButton(messages.getMessage(\"${cancelButton.lowercase().replace(" ", "_")}\"));")
            }

            if (informationString.isNotEmpty()) {
                appendLine("        $varName.setInformationString(messages.getMessage(\"${informationString.lowercase().replace(" ", "_")}\"));")
            }

            if (informationAlertTitle.isNotEmpty()) {
                appendLine("        $varName.setInformationAlertTitle(messages.getMessage(\"${informationAlertTitle.lowercase().replace(" ", "_")}\"));")
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
