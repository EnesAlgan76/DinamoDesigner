package org.jetbrains.plugins.designer.services

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.template.designer.components.TextFieldComponent
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class GeminiService(private val project: Project) {

    private val httpClient = HttpClient.newBuilder().build()
    private val gson = Gson()

    companion object {
        private const val GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"
        private const val API_KEY = "AIzaSyC4vF8rN_tW19_amk0273pWg3ewK2pY_hw"
    }

    /**
     * Generates screen components based on user description
     * @param userPrompt User's description of what they want
     * @return JSON string with component array or null if error
     */
    fun generateScreenComponents(userPrompt: String): String? {

        val systemPrompt = buildSystemPrompt()
        val fullPrompt = """
$systemPrompt

User Request: $userPrompt

Generate ONLY valid JSON response with components array. No explanation, no markdown, just JSON.
        """.trimIndent()

        try {
            val requestBody = mapOf(
                "contents" to listOf(
                    mapOf("parts" to listOf(mapOf("text" to fullPrompt)))
                )
            )

            val request = HttpRequest.newBuilder()
                .uri(URI.create("$GEMINI_API_URL?key=$API_KEY"))
                .header("Content-Type", "application/json")
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

    /**
     * Extracts JSON content from Gemini API response
     */
    private fun extractJsonFromResponse(responseBody: String): String? {
        try {
            val jsonResponse = JsonParser.parseString(responseBody).asJsonObject
            val candidates = jsonResponse.getAsJsonArray("candidates")

            if (candidates != null && candidates.size() > 0) {
                val firstCandidate = candidates[0].asJsonObject
                val content = firstCandidate.getAsJsonObject("content")
                val parts = content.getAsJsonArray("parts")

                if (parts != null && parts.size() > 0) {
                    val text = parts[0].asJsonObject.get("text").asString

                    // Clean up markdown code blocks if present
                    val cleanText = text
                        .replace("```json", "")
                        .replace("```", "")
                        .trim()

                    return cleanText
                }
            }

            return null
        } catch (e: Exception) {
            throw RuntimeException("Failed to parse API response: ${e.message}", e)
        }
    }

    /**
     * Builds the system prompt with component schema and examples
     */
    private fun buildSystemPrompt(): String {
        return """
You are a mobile banking screen designer assistant. Your task is to generate screen components based on user descriptions.

IMPORTANT: All properties are OPTIONAL except where noted. Only include properties you need - missing properties will use default values. The user can edit all properties later.

AVAILABLE COMPONENTS:

1. ${TextFieldComponent.type} - Text input field
   Example: {"type": "${TextFieldComponent.type}", "properties": {"identifier": "RECEIVER_NAME", "title": "Alıcı Adı", "required": true}}
   All Properties:
   - identifier: string (UPPERCASE_SNAKE_CASE, default: "TEXTFIELD")
   - title: string (default: "Enter text")
   - maxLength: number (default: 100)
   - required: boolean (default: false)
   - placeholder: string (default: "")
   - textType: "AlphaNumeric"|"OnlyNumber"|"OnlyAlpha"|"Email" (default: "AlphaNumeric")
   - keyboardType: "Default"|"NumberPad"|"EmailAddress"|"PhonePad"|"URL"|"DecimalPad" (default: "Default")
   - predefinedText: string (default: "")
   - informationString: string (default: "")
   - informationTitle: string (default: "")
   - disable: boolean (default: false)

2. AMOUNT_FIELD - Currency amount input
   Example: {"type": "AMOUNT_FIELD", "properties": {"identifier": "TRANSFER_AMOUNT", "title": "Tutar"}}
   All Properties:
   - identifier: string (default: "AMOUNT")
   - title: string (default: "Amount")
   - currencyCode: string (default: "TL")
   - required: boolean (default: true)
   - hideFraction: boolean (default: false)

3. COMBO_BOX - Dropdown selector
   Example: {"type": "COMBO_BOX", "properties": {"identifier": "FROM_ACCOUNT", "title": "Hesap Seçin"}}
   All Properties:
   - identifier: string (default: "COMBOBOX")
   - title: string (default: "Select option")
   - items: string (comma-separated, default: "Option 1,Option 2,Option 3")
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
   Example: {"type": "CHECKBOX", "properties": {"identifier": "TERMS_AGREE", "text": "Şartları kabul ediyorum", "required": true}}
   All Properties:
   - identifier: string (default: "CHECKBOX")
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
   Example: {"type": "PAYMENT_TOOL", "properties": {"identifier": "PAYMENT_METHOD", "title": "Ödeme Yöntemi"}}
   All Properties:
   - identifier: string (default: "PAYMENTTOOL")
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