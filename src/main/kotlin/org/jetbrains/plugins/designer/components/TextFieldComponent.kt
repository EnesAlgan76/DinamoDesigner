package org.jetbrains.plugins.template.designer.components

import com.intellij.openapi.project.Project
import org.jetbrains.plugins.designer.codegen.TFIdentifierManager
import org.jetbrains.plugins.designer.codegen.TFMessagesManager
import org.jetbrains.plugins.designer.components.PropertyDescriptor
import org.jetbrains.plugins.designer.models.ComponentInstance
import org.jetbrains.plugins.designer.models.Screen
import java.awt.Image
import javax.swing.ImageIcon
import kotlin.collections.set

object TextFieldComponent : ComponentDefinition {
    override val type = "TextFieldInput"
    override val displayName = "Text Field"

    override val propertyDescriptors = listOf(
        PropertyDescriptor.Text("identifier", "TEXTFIELD"),
        PropertyDescriptor.Text("title", "Enter text"),
        PropertyDescriptor.Number("maxLength", 200),
        PropertyDescriptor.Boolean("required", false),
        PropertyDescriptor.Enum("textType", "AlphaNumeric", listOf("None", "OnlyNumber", "OnlyNumeric", "AlphaNumeric", "AlphaNumericWithTurkishCharacter", "Alphabet", "TaxSerialNumber", "AlphaNumericWithBrackets")),
        PropertyDescriptor.Enum("stringCaseType", "None", listOf("None", "Upper", "Lower")),
        PropertyDescriptor.Enum("keyboardType", "Default", listOf("Default", "NumberPad", "DecimalPad", "NumbersAndPunctuation", "EmailAddress")),
        PropertyDescriptor.Text("predefinedText", ""),
        PropertyDescriptor.ConditionalGroup(
            k = "rightButton",
            d = false,
            childProperties = listOf(
                PropertyDescriptor.Text("title", ""),
                PropertyDescriptor.Text("identifier", ""),
                PropertyDescriptor.Text("value", ""),
                PropertyDescriptor.Text("dataSourceIdentifier", "")
            ),
        ),
        PropertyDescriptor.Text("informationString", ""),
        PropertyDescriptor.Text("informationAlertTitle", ""),
        PropertyDescriptor.Boolean("highlightedError", false),
        PropertyDescriptor.Boolean("disable", false)
    )

    override fun getDisplayIcon(size: Int?): ImageIcon {
        return try {
            val resource = javaClass.getResource("/icons/textfield.png")
                ?: throw IllegalArgumentException("Icon not found: /icons/textfield.png")

            if (size != null) {
                val originalIcon = ImageIcon(resource)
                val scaledImage = originalIcon.image.getScaledInstance(size, size, Image.SCALE_SMOOTH)
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
        val maxLength = props["maxLength"] as? Int ?: 200
        val required = props["required"] as? Boolean ?: false
        val textType = props["textType"] as? String ?: "AlphaNumeric"
        val stringCaseType = props["stringCaseType"] as? String ?: "None"
        val keyboardType = props["keyboardType"] as? String ?: "Default"
        val predefinedText = props["predefinedText"] as? String ?: ""

        val rightButtonMap = props["rightButton"] as? Map<*, *>
        val rightButtonTitle = rightButtonMap?.get("title") as? String ?: ""
        val rightButtonIdentifier = rightButtonMap?.get("identifier") as? String ?: ""
        val rightButtonValue = rightButtonMap?.get("value") as? String ?: ""
        val rightButtonDataSourceIdentifier = rightButtonMap?.get("dataSourceIdentifier") as? String ?: ""

        val informationString = props["informationString"] as? String ?: ""
        val informationAlertTitle = props["informationAlertTitle"] as? String ?: ""
        val highlightedError = props["highlightedError"] as? Boolean ?: false
        val disable = props["disable"] as? Boolean ?: false

        return buildString {
            appendLine("        TFComponentTextFieldInput $varName = new TFComponentTextFieldInput($identifier);")
            val titleMessage = TFMessagesManager.getOrCreateMessage(project, title)
            appendLine("        $varName.setTitle($titleMessage);")
            appendLine("        $varName.setMaxLength($maxLength);")

            if (predefinedText.isNotEmpty()) {
                appendLine("        $varName.setPredefinedText(\"$predefinedText\");")
            }

            if (textType != "AlphaNumeric") {
                appendLine("        $varName.setTextType(TFTextType.$textType.name());")
            }

            if (stringCaseType != "None") {
                appendLine("        $varName.setStringCaseType(TFStringCaseType.$stringCaseType.name());")
            }

            if (keyboardType != "Default") {
                appendLine("        $varName.setKeyboardType(TFKeyboardType.$keyboardType.name());")
            }

            if (rightButtonMap != null && rightButtonTitle.isNotEmpty()) {
                appendLine("        TFRightButtonModel rightButtonModel = new TFRightButtonModel();")
                val titleMsg = TFMessagesManager.getOrCreateMessage(project, rightButtonTitle)
                appendLine("        rightButtonModel.setTitle($titleMsg);")

                if (rightButtonIdentifier.isNotEmpty()) {
                    val buttonIdentifier = TFIdentifierManager.getOrCreateIdentifier(project, rightButtonIdentifier)
                    appendLine("        rightButtonModel.setIdentifier($buttonIdentifier);")
                }

                if (rightButtonValue.isNotEmpty()) {
                    appendLine("        rightButtonModel.setValue(\"$rightButtonValue\");")
                }

                if (rightButtonDataSourceIdentifier.isNotEmpty()) {
                    appendLine("        rightButtonModel.setDataSourceIdentifier(\"$rightButtonDataSourceIdentifier\");")
                }

                appendLine("        $varName.setRightButton(rightButtonModel);")
            }

            if (required) {
                val requiredMessage = TFMessagesManager.getOrCreateMessage(project, "${title} is required")
                appendLine("        $varName.setValidation(true, $requiredMessage);")
            }

            if (informationString.isNotEmpty()) {
                val infoStringMessage = TFMessagesManager.getOrCreateMessage(project, informationString)
                appendLine("        $varName.setInformationString($infoStringMessage);")
            }

            if (informationAlertTitle.isNotEmpty()) {
                val infoTitleMessage = TFMessagesManager.getOrCreateMessage(project, informationAlertTitle)
                appendLine("        $varName.setInformationTitle($infoTitleMessage);")
            }

            if (highlightedError) {
                appendLine("        $varName.setHighlightErrorEnable(true);")
            }

            if (disable) {
                appendLine("        $varName.setDisable(true);")
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
