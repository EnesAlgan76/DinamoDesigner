package org.jetbrains.plugins.designer.services

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.intellij.openapi.project.Project
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

AVAILABLE COMPONENTS:

1. TEXT_FIELD - Text input field
   Example: {"type": "TEXT_FIELD", "properties": {"identifier": "RECEIVER_NAME", "title": "Alıcı Adı", "maxLength": 50, "required": true, "textType": "OnlyAlpha"}}
   Fields: identifier (UPPERCASE_SNAKE_CASE), title, maxLength, required (true/false), textType (AlphaNumeric|OnlyNumber|OnlyAlpha|Email), keyboardType (Default|NumberPad|EmailAddress|PhonePad)

2. AMOUNT_FIELD - Currency amount input
   Example: {"type": "AMOUNT_FIELD", "properties": {"identifier": "TRANSFER_AMOUNT", "title": "Transfer Tutarı", "currencyCode": "TRY", "required": true, "hideFraction": false}}
   Fields: identifier, title, currencyCode (TRY|USD|EUR), required, hideFraction (true/false)

3. COMBO_BOX - Dropdown selector
   Example: {"type": "COMBO_BOX", "properties": {"identifier": "FROM_ACCOUNT", "title": "Gönderen Hesap", "required": true}}
   Fields: identifier, title, required

4. DATE_PICKER - Date selection
   Example: {"type": "DATE_PICKER", "properties": {"identifier": "BIRTH_DATE", "title": "Doğum Tarihi", "validation": true}}
   Fields: identifier, title, validation (true/false)

5. CHECKBOX - Checkbox with optional popup
   Example: {"type": "CHECKBOX", "properties": {"identifier": "TERMS_AGREE", "text": "Şartları kabul ediyorum", "underlineText": "Kullanım Şartları", "required": true, "showPopUp": true, "popUpTitle": "Kullanım Şartları"}}
   Fields: identifier, text, underlineText (optional), required, showPopUp (true/false), popUpTitle (optional)

6. BUTTON - Action button
   Example: {"type": "BUTTON", "properties": {"identifier": "CONTINUE_BTN", "text": "Devam Et", "buttonType": "PRIMARY", "targetScreen": "CONFIRM_SCREEN"}}
   Fields: identifier, text, buttonType (PRIMARY|SECONDARY), targetScreen (optional, for navigation)

7. PAYMENT_TOOL - Payment method selector
   Example: {"type": "PAYMENT_TOOL", "properties": {"identifier": "PAYMENT_METHOD", "title": "Ödeme Yöntemi", "paymentToolType": "Both", "required": true}}
   Fields: identifier, title, paymentToolType (Account|CreditCard|Both), required

RULES:
- All identifiers MUST be in UPPERCASE_SNAKE_CASE format (e.g., TRANSFER_AMOUNT, FROM_ACCOUNT)
- Return ONLY a valid JSON object with "components" array
- Each component must have "type" and "properties"
- Do NOT include any explanations, markdown formatting, or extra text
- Always include at least one BUTTON component
- For forms, use appropriate field types based on data (amounts → AMOUNT_FIELD, accounts → COMBO_BOX, etc.)

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