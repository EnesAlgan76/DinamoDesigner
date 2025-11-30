package org.jetbrains.plugins.template.designer.components

import com.intellij.openapi.project.Project
import org.jetbrains.plugins.designer.codegen.TFIdentifierManager
import org.jetbrains.plugins.designer.codegen.TFMessagesManager
import org.jetbrains.plugins.designer.components.PropertyDescriptor
import org.jetbrains.plugins.designer.models.ComponentInstance
import org.jetbrains.plugins.designer.models.Screen
import javax.swing.ImageIcon

object CheckBoxComponent : ComponentDefinition {
    override val type = "CheckBox"
    override val displayName = "CheckBox"

    override val propertyDescriptors = listOf(
        PropertyDescriptor.Text("identifier", CheckBoxComponent.type),
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
            ImageIcon()
        }
    }

    override fun generateCode(project: Project, component: ComponentInstance, allScreens: List<Screen>): String {
        val props = component.properties
        val identifierValue = props["identifier"] as? String ?: CheckBoxComponent.type
        val identifier = TFIdentifierManager.getOrCreateIdentifier(project, identifierValue)
        val varName = identifierValue.lowercase().replace("_", "")
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
            appendLine("        TFComponentCheckBox $varName = new TFComponentCheckBox($identifier);")
            val textMessage = TFMessagesManager.getOrCreateMessage(project, text)
            appendLine("        $varName.setText($textMessage);")

            if (underlineText.isNotEmpty()) {
                val underlineMessage = TFMessagesManager.getOrCreateMessage(project, underlineText)
                appendLine("        $varName.setUnderlineText($underlineMessage);")
            }

            if (descriptionText.isNotEmpty()) {
                val descriptionMessage = TFMessagesManager.getOrCreateMessage(project, descriptionText)
                appendLine("        $varName.setDescriptionText($descriptionMessage);")
            }

            appendLine("        $varName.setChecked($checked);")

            if (showPopUp) {
                appendLine("        $varName.setShowPopUp(true);")
                val popUpTitleMessage = TFMessagesManager.getOrCreateMessage(project, popUpTitle)
                appendLine("        $varName.setPopUpTitle($popUpTitleMessage);")
                val popUpTextMessage = TFMessagesManager.getOrCreateMessage(project, popUpText)
                appendLine("        $varName.setInformationMessage($popUpTextMessage);")
                val continueButtonMessage = TFMessagesManager.getOrCreateMessage(project, continueButton)
                appendLine("        $varName.setContinueButton($continueButtonMessage);")
                val cancelButtonMessage = TFMessagesManager.getOrCreateMessage(project, cancelButton)
                appendLine("        $varName.setCancelButton($cancelButtonMessage);")
            }

            if (informationString.isNotEmpty()) {
                val infoStringMessage = TFMessagesManager.getOrCreateMessage(project, informationString)
                appendLine("        $varName.setInformationString($infoStringMessage);")
            }

            if (informationAlertTitle.isNotEmpty()) {
                val infoAlertMessage = TFMessagesManager.getOrCreateMessage(project, informationAlertTitle)
                appendLine("        $varName.setInformationAlertTitle($infoAlertMessage);")
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
