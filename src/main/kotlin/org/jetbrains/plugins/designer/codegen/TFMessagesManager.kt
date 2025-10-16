package org.jetbrains.plugins.designer.codegen

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import org.jetbrains.plugins.designer.settings.PluginSettings
import java.io.File
import java.nio.charset.StandardCharsets

object TFMessagesManager {

    private val messagesCache = mutableMapOf<String, String>()
    private var cacheInitialized = false


    fun getOrCreateMessage(project: Project, messageValue: String): String {
        if (!cacheInitialized) {
            loadMessagesFromFile(project)
        }

        val existingKey = messagesCache[messageValue]
        if (existingKey != null) {
            return "messages.getMessage(\"$existingKey\")"
        }

        val messageKey = generateMessageKey(messageValue)

        addMessageToFile(project, messageKey, messageValue)

        messagesCache[messageValue] = messageKey

        return "messages.getMessage(\"$messageKey\")"
    }


    private fun generateMessageKey(messageValue: String): String {
        return messageValue
            .lowercase()
            .replace("ğ", "g")
            .replace("ü", "u")
            .replace("ş", "s")
            .replace("ı", "i")
            .replace("ö", "o")
            .replace("ç", "c")
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
    }


    private fun loadMessagesFromFile(project: Project) {
        try {
            val basePath = project.basePath ?: return
            val settings = PluginSettings.getInstance(project)
            val messagesFilePath = File(basePath, settings.tfMessagesPath)

            if (!messagesFilePath.exists()) {
                cacheInitialized = true
                return
            }

            val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(messagesFilePath)

            val content = if (virtualFile != null) {
                // Unicode escape sequences'leri decode et
                String(virtualFile.contentsToByteArray(), Charsets.ISO_8859_1).unescapeUnicode()
            } else {
                messagesFilePath.readText(Charsets.ISO_8859_1).unescapeUnicode()
            }

            content.lines().forEach { line ->
                val trimmedLine = line.trim()
                if (trimmedLine.isEmpty() || trimmedLine.startsWith("#")) {
                    return@forEach
                }

                val separatorIndex = trimmedLine.indexOf('=')
                if (separatorIndex > 0) {
                    val key = trimmedLine.substring(0, separatorIndex).trim()
                    val value = trimmedLine.substring(separatorIndex + 1).trim()

                    if (key.isNotEmpty() && value.isNotEmpty()) {
                        messagesCache[value] = key
                    }
                }
            }

            cacheInitialized = true
        } catch (e: Exception) {
            e.printStackTrace()
            cacheInitialized = true
        }
    }


    private fun addMessageToFile(project: Project, messageKey: String, value: String) {
        ApplicationManager.getApplication().invokeLater {
            ApplicationManager.getApplication().runWriteAction {
                try {
                    val basePath = project.basePath ?: return@runWriteAction
                    val settings = PluginSettings.getInstance(project)
                    val messagesFilePath = File(basePath, settings.tfMessagesPath)

                    if (!messagesFilePath.exists()) {
                        return@runWriteAction
                    }

                    val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(messagesFilePath)
                        ?: return@runWriteAction

                    var content = String(virtualFile.contentsToByteArray(), Charsets.ISO_8859_1)
                    val decodedContent = content.unescapeUnicode()

                    // Aynı value zaten varsa ekleme yapma
                    val lines = decodedContent.lines()
                    for (line in lines) {
                        val trimmedLine = line.trim()
                        if (trimmedLine.isEmpty() || trimmedLine.startsWith("#")) {
                            continue
                        }

                        val separatorIndex = trimmedLine.indexOf('=')
                        if (separatorIndex > 0) {
                            val existingValue = trimmedLine.substring(separatorIndex + 1).trim()
                            if (existingValue == value) {
                                return@runWriteAction
                            }
                        }
                    }

                    // Aynı key varsa ekleme yapma
                    if (lines.any { it.trim().startsWith("$messageKey=") }) {
                        return@runWriteAction
                    }

                    // Dosya boş satırla bitiyorsa
                    if (content.isNotEmpty() && !content.endsWith("\n")) {
                        content += "\n"
                    }

                    // Unicode escape ile encode et
                    val escapedValue = value.escapeUnicode()
                    val newLine = "$messageKey=$escapedValue\n"
                    content += newLine

                    virtualFile.setBinaryContent(content.toByteArray(Charsets.ISO_8859_1))
                    virtualFile.refresh(false, false)

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }


    // Unicode escape sequences'leri decode et (\uXXXX -> karakter)
    private fun String.unescapeUnicode(): String {
        val regex = Regex("""\\u([0-9a-fA-F]{4})""")
        return regex.replace(this) { matchResult ->
            matchResult.groupValues[1].toInt(16).toChar().toString()
        }
    }

    // Türkçe karakterleri Unicode escape'e çevir (karakter -> \uXXXX)
    private fun String.escapeUnicode(): String {
        return this.map { char ->
            when {
                char.code > 127 -> "\\u%04x".format(char.code)
                else -> char.toString()
            }
        }.joinToString("")
    }


    fun clearCache() {
        messagesCache.clear()
        cacheInitialized = false
    }


    fun hasMessage(project: Project, messageValue: String): Boolean {
        if (!cacheInitialized) {
            loadMessagesFromFile(project)
        }
        return messagesCache.containsKey(messageValue)
    }


    fun getMessageKey(project: Project, messageValue: String): String? {
        if (!cacheInitialized) {
            loadMessagesFromFile(project)
        }
        return messagesCache[messageValue]
    }
}