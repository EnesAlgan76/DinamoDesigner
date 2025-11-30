package org.jetbrains.plugins.designer.services

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.designer.components.AmountFieldComponent
import org.jetbrains.plugins.designer.config.ApiKeyConfig
import org.jetbrains.plugins.template.designer.components.CheckBoxComponent
import org.jetbrains.plugins.template.designer.components.ComboBoxComponent
import org.jetbrains.plugins.template.designer.components.PaymentToolComponent
import org.jetbrains.plugins.template.designer.components.TextFieldComponent
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.Base64
import javax.imageio.ImageIO

class OpenAiService(private val project: Project) {

    private val httpClient = HttpClient.newBuilder().build()
    private val gson = Gson()

    companion object {
        private const val OPENAI_API_URL = "https://api.openai.com/v1/chat/completions"
    }

    fun generateScreenComponents(userPrompt: String, image: BufferedImage? = null): String? {
        val systemPrompt = buildSystemPrompt(image != null)
        val fullPrompt = if (image != null) {
            """
$systemPrompt

User Request: $userPrompt

IMPORTANT: Analyze the provided screenshot image and recreate the UI using ONLY the available components listed above.
- Components should be listed from TOP to BOTTOM as they appear in the image
- Match the layout, spacing, and order of elements
- Use appropriate component types based on visual appearance
- Generate ONLY valid JSON response with components array. No explanation, no markdown, just JSON.
        """.trimIndent()
        } else {
            """
$systemPrompt

User Request: $userPrompt

Generate ONLY valid JSON response with components array. No explanation, no markdown, just JSON.
        """.trimIndent()
        }

        try {
            val messages = mutableListOf<Map<String, Any>>()

            if (image != null) {
                val base64Image = encodeImageToBase64(image)
                messages.add(mapOf(
                    "role" to "user",
                    "content" to listOf(
                        mapOf("type" to "text", "text" to fullPrompt),
                        mapOf(
                            "type" to "image_url",
                            "image_url" to mapOf(
                                "url" to "data:image/jpeg;base64,$base64Image"
                            )
                        )
                    )
                ))
            } else {
                messages.add(mapOf(
                    "role" to "user",
                    "content" to fullPrompt
                ))
            }

            val requestBody = mapOf(
                "model" to ApiKeyConfig.DEFAULT_MODEL,
                "messages" to messages,
                "max_tokens" to 4096,
                "temperature" to 0.7
            )

            val request = HttpRequest.newBuilder()
                .uri(URI.create(OPENAI_API_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer ${ApiKeyConfig.getApiKey()}")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                .build()

            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

            if (response.statusCode() == 200) {
                return extractJsonFromResponse(response.body())
            } else {
                throw RuntimeException("API Error ${response.statusCode()}: ${response.body()}")
            }

        } catch (e: Exception) {
            throw RuntimeException("Failed to generate components: ${e.message}", e)
        }
    }

    private fun encodeImageToBase64(image: BufferedImage): String {
        val outputStream = ByteArrayOutputStream()

        val rgbImage = if (image.type == BufferedImage.TYPE_INT_ARGB || image.type == BufferedImage.TYPE_4BYTE_ABGR) {
            val convertedImage = BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_RGB)
            val g2d = convertedImage.createGraphics()
            g2d.drawImage(image, 0, 0, null)
            g2d.dispose()
            convertedImage
        } else {
            image
        }

        val success = ImageIO.write(rgbImage, "jpeg", outputStream)
        if (!success) {
            throw RuntimeException("Failed to encode image to JPEG")
        }

        val bytes = outputStream.toByteArray()
        if (bytes.isEmpty()) {
            throw RuntimeException("Image encoding resulted in empty bytes")
        }

        return Base64.getEncoder().encodeToString(bytes)
    }

    private fun extractJsonFromResponse(responseBody: String): String? {
        try {
            val jsonResponse = JsonParser.parseString(responseBody).asJsonObject
            val choices = jsonResponse.getAsJsonArray("choices")

            if (choices != null && choices.size() > 0) {
                val firstChoice = choices[0].asJsonObject
                val message = firstChoice.getAsJsonObject("message")
                val content = message.get("content").asString

                val cleanText = content
                    .replace("```json", "")
                    .replace("```", "")
                    .trim()

                return cleanText
            }

            return null
        } catch (e: Exception) {
            throw RuntimeException("Failed to parse API response: ${e.message}", e)
        }
    }

    private fun buildSystemPrompt(hasImage: Boolean = false): String {
        val imageInstruction = if (hasImage) {
            """
You are a mobile banking screen designer assistant. Your task is to analyze the provided screenshot image and recreate the UI using the available components.

IMPORTANT IMAGE ANALYSIS INSTRUCTIONS:
- Carefully examine the screenshot to identify all UI elements from TOP to BOTTOM
- List components in the EXACT ORDER they appear in the image (top to bottom)
- Match the visual appearance to the appropriate component type
- Text fields → TEXTFIELD, Amount inputs → AMOUNT_FIELD, Dropdowns → COMBO_BOX, Buttons → BUTTON, etc.
- Extract visible text for titles, labels, and button text
- If you see a component that doesn't match available types, use the closest match
            """.trimIndent()
        } else {
            "You are a mobile banking screen designer assistant. Your task is to generate screen components based on user descriptions."
        }

        return """
$imageInstruction

IMPORTANT: All properties are OPTIONAL except where noted. Only include properties you need - missing properties will use default values. The user can edit all properties later.

AVAILABLE COMPONENTS:

1. ${TextFieldComponent.type} - Text input field
   Example: {"type": "${TextFieldComponent.type}", "properties": {"identifier": "RECEIVER_NAME", "title": "Alıcı Adı", "required": true}}
   All Properties:
   - identifier: string (UPPERCASE_SNAKE_CASE, default: "TEXTFIELD")
   - title: string (default: "Enter text")
   - maxLength: number (default: 200)
   - required: boolean (default: false)
   - textType: "None"|"OnlyNumber"|"OnlyNumeric"|"AlphaNumeric"|"AlphaNumericWithTurkishCharacter"|"Alphabet"|"TaxSerialNumber"|"AlphaNumericWithBrackets" (default: "AlphaNumeric")
   - stringCaseType: "None"|"Upper"|"Lower" (default: "None")
   - keyboardType: "Default"|"NumberPad"|"DecimalPad"|"NumbersAndPunctuation"|"EmailAddress" (default: "Default")
   - predefinedText: string (default: "")
   - rightButtonTitle: string (default: "")
   - rightButtonValue: string (default: "")
   - informationString: string (default: "")
   - informationAlertTitle: string (default: "")
   - highlightedError: boolean (default: false)
   - disable: boolean (default: false)

2. ${AmountFieldComponent.type} - Currency amount input
   Example: {"type": "${AmountFieldComponent.type}", "properties": {"identifier": "TRANSFER_AMOUNT", "title": "Tutar"}}
   All Properties:
   - identifier: string (default: "AMOUNT")
   - title: string (default: "Amount")
   - currencyCode: string (default: "TL")
   - required: boolean (default: true)
   - hideFraction: boolean (default: false)

3. COMBO_BOX - Dropdown selector
   Example: {"type": "${ComboBoxComponent.type}", "properties": {"identifier": "FROM_ACCOUNT", "title": "Hesap Seçin", "items": "Hesap 1,Hesap 2,Hesap 3"}}
   All Properties:
   - identifier: string (default: "${ComboBoxComponent.type}")
   - title: string (default: "Select option")
   - disable: boolean (default: false)
   - items: string (comma-separated values like "Item1,Item2,Item3", default: "Option 1,Option 2,Option 3")
   - selectedIndex: number (default: 0)
   - placeholder: string (default: "Please select")
   - showPlaceholderAsFirstItem: boolean (default: false)
   - required: boolean (default: false)
   - informationString: string (default: "")
   - informationTitle: string (default: "")

4. DATE_PICKER - Date selection
   Example: {"type": "DATE_PICKER", "properties": {"identifier": "BIRTH_DATE", "title": "Doğum Tarihi"}}
   All Properties:
   - identifier: string (default: "DATEPICKER")
   - title: string (default: "Select date")
   - validation: boolean (default: true)
   - minDate: string (default: "today")
   - maxDate: string (default: "")

5. CHECKBOX - Checkbox with optional popup
   Example: {"type": "${CheckBoxComponent.type}", "properties": {"identifier": "TERMS_AGREE", "text": "Şartları kabul ediyorum", "required": true}}
   All Properties:
   - identifier: string (default: "${CheckBoxComponent.type}")
   - text: string (default: "Checkbox Text")
   - underlineText: string (default: "")
   - descriptionText: string (default: "")
   - checked: boolean (default: false)
   - required: boolean (default: false)
   - showPopUp: boolean (default: false)
   - popUpTitle: string (default: "")
   - popUpText: string (default: "")
   - continueButton: string (default: "Continue")
   - cancelButton: string (default: "Cancel")
   - informationString: string (default: "")
   - informationAlertTitle: string (default: "")

6. BUTTON - Action button
   Example: {"type": "BUTTON", "properties": {"identifier": "CONTINUE_BTN", "text": "Devam Et"}}
   All Properties:
   - identifier: string (default: "BUTTON")
   - text: string (default: "Click Me")
   - buttonType: string (default: "PRIMARY")
   - targetScreen: string (screen ID for navigation, default: "")

7. PAYMENT_TOOL - Payment method selector
   Example: {"type": "${PaymentToolComponent.type}", "properties": {"identifier": "PAYMENT_METHOD", "title": "Ödeme Yöntemi"}}
   All Properties:
   - identifier: string (default: "${PaymentToolComponent.type}")
   - title: string (default: "Select payment method")
   - paymentToolType: "Account"|"CreditCard"|"Both" (default: "Both")
   - required: boolean (default: true)
   - screenTitle: string (default: "")
   - screenInfo: string (default: "")

RULES:
- All identifiers MUST be in UPPERCASE_SNAKE_CASE format (e.g., TRANSFER_AMOUNT, FROM_ACCOUNT)
- Return ONLY a valid JSON object with "components" array
- Each component must have "type" and "properties"
- Do NOT include any explanations, markdown formatting, or extra text
- Always include at least one BUTTON component
- For forms, use appropriate field types based on data (amounts → AMOUNT_FIELD, accounts → COMBO_BOX, etc.)
- Only include properties you actually need to set - omitted properties will automatically use their defaults

RESPONSE FORMAT:
{
  "components": [
    {"type": "...", "properties": {...}},
    {"type": "...", "properties": {...}}
  ]
}
        """.trimIndent()
    }
}