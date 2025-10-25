package org.jetbrains.plugins.designer.services

import com.google.api.gax.core.FixedCredentialsProvider
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.speech.v1.*
import com.google.protobuf.ByteString
import com.intellij.openapi.project.Project
import java.io.File
import java.io.FileInputStream

class SpeechToTextService(private val project: Project) {

    companion object {
        private const val CREDENTIALS_PATH = "credentials/google-cloud-credentials.json"
    }

    /**
     * Converts audio file to text using Google Speech-to-Text API
     * @param audioFile The audio file to transcribe
     * @return Transcribed text or null if error
     */
    fun transcribeAudio(audioFile: File): String? {
        try {
            // Load credentials from JSON file
            val credentialsFile = File("/Users/enesalgan/Projeler/DinamoDesigner/credentials/google-cloud-credentials.json")

            if (!credentialsFile.exists()) {
                throw RuntimeException(
                    "Credentials dosyası bulunamadı: ${credentialsFile.absolutePath}\n" +
                    "Lütfen Google Cloud service account JSON dosyasını bu konuma kaydedin."
                )
            }

            val credentials = GoogleCredentials.fromStream(FileInputStream(credentialsFile))

            // Create Speech client with credentials
            val settings = SpeechSettings.newBuilder()
                .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                .build()

            SpeechClient.create(settings).use { speechClient ->
                // Read audio file
                val audioBytes = audioFile.readBytes()
                val audioData = ByteString.copyFrom(audioBytes)

                // Configure recognition
                val config = RecognitionConfig.newBuilder()
                    .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                    .setSampleRateHertz(16000)
                    .setLanguageCode("tr-TR")
                    .setEnableAutomaticPunctuation(true)
                    .build()

                val audio = RecognitionAudio.newBuilder()
                    .setContent(audioData)
                    .build()

                // Perform speech recognition
                val response = speechClient.recognize(config, audio)
                val results = response.resultsList

                // Extract transcript from first result
                if (results.isNotEmpty()) {
                    val alternatives = results[0].alternativesList
                    if (alternatives.isNotEmpty()) {
                        return alternatives[0].transcript
                    }
                }

                return null
            }

        } catch (e: Exception) {
            throw RuntimeException("Ses metne dönüştürülemedi: ${e.message}", e)
        }
    }
}
