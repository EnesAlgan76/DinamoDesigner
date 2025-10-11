package org.jetbrains.plugins.designer.components

import org.jetbrains.plugins.designer.models.ComponentInstance
import org.jetbrains.plugins.designer.models.Screen
import org.jetbrains.plugins.template.designer.components.ComponentDefinition
import java.awt.Image
import javax.swing.ImageIcon

object AmountFieldComponent : ComponentDefinition {
    override val type = "AMOUNT_FIELD"
    override val displayName = "Amount Field"

    override val propertyDescriptors = listOf(
        PropertyDescriptor.Text("identifier", "AMOUNT"),
        PropertyDescriptor.Text("title", "Amount"),
        PropertyDescriptor.Text("currencyCode", "TL"),
        PropertyDescriptor.Boolean("required", true),
        PropertyDescriptor.Boolean("hideFraction", false)
    )

    override fun getDisplayIcon(size: Int?): ImageIcon {
        return try {
            val resource = javaClass.getResource("/icons/textfield.png")
                ?: throw IllegalArgumentException("Icon not found")

            if (size != null) {
                val originalIcon = ImageIcon(resource)
                val scaledImage = originalIcon.image.getScaledInstance(size, size, Image.SCALE_SMOOTH)
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
        val identifier = props["identifier"] as? String ?: "AMOUNT"
        val varName = identifier.lowercase().replace("_", "")
        val title = props["title"] as? String ?: "Amount"
        val currencyCode = props["currencyCode"] as? String ?: "TL"
        val required = props["required"] as? Boolean ?: true
        val hideFraction = props["hideFraction"] as? Boolean ?: false

        return buildString {
            appendLine("        TFComponentAmountTextFieldInput $varName = new TFComponentAmountTextFieldInput(\"$identifier\");")
            appendLine("        $varName.setTitle(messages.getMessage(\"${title.lowercase().replace(" ", "_")}\"));")
            appendLine("        $varName.setCurrencyCode(CurrencyType._$currencyCode);")
            if (hideFraction) {
                appendLine("        $varName.setHideFraction(true);")
            }
            if (required) {
                appendLine("        $varName.setValidation(true, messages.getMessage(\"${title.lowercase().replace(" ", "_")}_required_message\"));")
            }
            appendLine("        rowViewModelList.add($varName);")
        }
    }

    override fun validateProperties(properties: Map<String, Any>): Boolean {
        val identifier = properties["identifier"] as? String ?: return false
        val currencyCode = properties["currencyCode"] as? String ?: return false

        return identifier.isNotBlank() && currencyCode.isNotBlank()
    }
}
