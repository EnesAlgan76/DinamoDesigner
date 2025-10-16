package org.jetbrains.plugins.template.designer.components

import com.intellij.openapi.project.Project
import org.jetbrains.plugins.designer.codegen.TFIdentifierManager
import org.jetbrains.plugins.designer.codegen.TFMessagesManager
import org.jetbrains.plugins.designer.components.PropertyDescriptor
import org.jetbrains.plugins.designer.models.ComponentInstance
import org.jetbrains.plugins.designer.models.Screen
import javax.swing.ImageIcon

object PaymentToolComponent : ComponentDefinition {
    override val type = "PAYMENT_TOOL"
    override val displayName = "Payment Tool"

    override val propertyDescriptors = listOf(
        PropertyDescriptor.Text("identifier", "PAYMENTTOOL"),
        PropertyDescriptor.Text("title", "Select payment method"),
        PropertyDescriptor.Enum("paymentToolType", "Both",
            listOf("Account", "CreditCard", "Both")),
        PropertyDescriptor.Boolean("required", true),
        PropertyDescriptor.Text("screenTitle", ""),
        PropertyDescriptor.Text("screenInfo", "")
    )

    override fun getDisplayIcon(size: Int?): ImageIcon {
        return try {
            val resource = javaClass.getResource("/icons/paymenttool.png")
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
        val identifierValue = props["identifier"] as? String ?: "PAYMENTTOOL"
        val identifier = TFIdentifierManager.getOrCreateIdentifier(project, identifierValue)
        val varName = identifierValue.lowercase().replace("_", "")
        val title = props["title"] as? String ?: "Select payment method"
        val paymentToolType = props["paymentToolType"] as? String ?: "Both"
        val required = props["required"] as? Boolean ?: true

        return buildString {
            appendLine("        TFComponentPaymentToolSelection $varName = new TFComponentPaymentToolSelection($identifier);")
            val titleMessage = TFMessagesManager.getOrCreateMessage(project, title)
            appendLine("        $varName.setTitle($titleMessage);")
            appendLine("        $varName.setPaymentToolType(TFPaymentToolType.$paymentToolType);")
            val placeholderMessage = TFMessagesManager.getOrCreateMessage(project, "Account selection placeholder")
            appendLine("        $varName.setContainerPlaceholder($placeholderMessage);")

            if (required) {
                val requiredMessage = TFMessagesManager.getOrCreateMessage(project, "${title} is required")
                appendLine("        $varName.setValidation(true, $requiredMessage);")
            }

            appendLine()
            appendLine("        // TODO: Set accounts and credit cards")
            appendLine("        // List<PZTAccount> accountList = AccountUtil.getDebtorAccounts(service, messages, true, false, MobileJointAccountType.Individual, adcIntegrationRestClient, isProtocolREST());")
            appendLine("        // $varName.setAccounts(AccountUtil.convertAccountHashMap(accountList));")
            appendLine()
            appendLine("        // List<TFCreditCard> creditCards = CreditCardUtil.getCreditCardListForPayment(service, messages, PozitronCreditCardFunctionType.PaymentToolInstantWithBusiness, isProtocolREST(), adcIntegrationRestClient);")
            appendLine("        // $varName.setCreditCards(CreditCardUtil.convertCreditCardHashMap(creditCards));")
            appendLine()

            appendLine("        rowViewModelList.add($varName);")
        }
    }

    override fun validateProperties(properties: Map<String, Any>): Boolean {
        val identifier = properties["identifier"] as? String ?: return false
        val paymentToolType = properties["paymentToolType"] as? String ?: return false

        return identifier.isNotBlank() &&
                (paymentToolType == "Account" || paymentToolType == "CreditCard" || paymentToolType == "Both")
    }
}
