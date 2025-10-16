package org.jetbrains.plugins.template.designer.components

import com.intellij.openapi.project.Project
import org.jetbrains.plugins.designer.codegen.TFIdentifierManager
import org.jetbrains.plugins.designer.codegen.TFMessagesManager
import org.jetbrains.plugins.designer.components.PropertyDescriptor
import org.jetbrains.plugins.designer.models.ComponentInstance
import org.jetbrains.plugins.designer.models.Screen
import javax.swing.ImageIcon

object TextFieldComponent : ComponentDefinition {
    override val type = "TEXT_FIELD"
    override val displayName = "Text Field"

    override val propertyDescriptors = listOf(
        PropertyDescriptor.Text("identifier", "TEXTFIELD"),
        PropertyDescriptor.Text("title", "Enter text"),
        PropertyDescriptor.Number("maxLength", 100),
        PropertyDescriptor.Boolean("required", false),
        PropertyDescriptor.Text("placeholder", ""),
        PropertyDescriptor.Enum("textType", "AlphaNumeric",
            listOf("AlphaNumeric", "OnlyNumber", "OnlyAlpha", "Email")),
        PropertyDescriptor.Enum("keyboardType", "Default",
            listOf("Default", "NumberPad", "EmailAddress", "PhonePad", "URL", "DecimalPad")),
        PropertyDescriptor.Text("predefinedText", ""),
        PropertyDescriptor.Text("informationString", ""),
        PropertyDescriptor.Text("informationTitle", ""),
        PropertyDescriptor.Boolean("disable", false)
    )

    override fun getDisplayIcon(size: Int?): ImageIcon {
        return try {
            val resource = javaClass.getResource("/icons/textfield.png")
                ?: throw IllegalArgumentException("Icon not found: /icons/textfield.png")

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

    override fun generateCode(project: Project, component: ComponentInstance, allScreens: List<Screen>): String {
        val props = component.properties
        val identifierValue = props["identifier"] as? String ?: "TEXTFIELD"
        val identifier = TFIdentifierManager.getOrCreateIdentifier(project, identifierValue)
        val varName = identifierValue.lowercase().replace("_", "")
        val title = props["title"] as? String ?: "Enter text"
        val maxLength = props["maxLength"] as? Int ?: 100
        val required = props["required"] as? Boolean ?: false
        val textType = props["textType"] as? String ?: "AlphaNumeric"
        val keyboardType = props["keyboardType"] as? String ?: "Default"
        val predefinedText = props["predefinedText"] as? String ?: ""
        val informationString = props["informationString"] as? String ?: ""
        val informationTitle = props["informationTitle"] as? String ?: ""
        val disable = props["disable"] as? Boolean ?: false

        return buildString {
            appendLine("        TFComponentTextFieldInput $varName = new TFComponentTextFieldInput($identifier);")
            val titleMessage = TFMessagesManager.getOrCreateMessage(project, title)
            appendLine("        $varName.setTitle($titleMessage);")
            appendLine("        $varName.setMaxLength($maxLength);")

            if (textType != "AlphaNumeric") {
                appendLine("        $varName.setTextType(TFTextType.$textType.name());")
            }

            if (keyboardType != "Default") {
                appendLine("        $varName.setKeyboardType(TFKeyboardType.$keyboardType.name());")
            }

            if (predefinedText.isNotEmpty()) {
                appendLine("        $varName.setPredefinedText(\"$predefinedText\");")
            }

            if (informationString.isNotEmpty()) {
                val infoStringMessage = TFMessagesManager.getOrCreateMessage(project, informationString)
                appendLine("        $varName.setInformationString($infoStringMessage);")
            }

            if (informationTitle.isNotEmpty()) {
                val infoTitleMessage = TFMessagesManager.getOrCreateMessage(project, informationTitle)
                appendLine("        $varName.setInformationTitle($infoTitleMessage);")
            }

            if (disable) {
                appendLine("        $varName.setDisable(true);")
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
        val maxLength = properties["maxLength"] as? Int ?: return false

        return identifier.isNotBlank() && maxLength > 0
    }
}
