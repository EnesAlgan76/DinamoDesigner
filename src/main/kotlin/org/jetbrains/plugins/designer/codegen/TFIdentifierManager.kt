package org.jetbrains.plugins.designer.codegen

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import org.jetbrains.plugins.designer.settings.PluginSettings
import java.io.File

/**
 * Manages TFIdentifier class operations - reading existing identifiers and adding new ones
 */
object TFIdentifierManager {

    private val identifierCache = mutableMapOf<String, String>()
    private var cacheInitialized = false

    /**
     * Gets an identifier constant name. If the value exists in TFIdentifier class, returns the existing constant name.
     * Otherwise, creates a new constant.
     */
    fun getOrCreateIdentifier(project: Project, identifierValue: String): String {
        if (!cacheInitialized) {
            loadIdentifiersFromFile(project)
        }

        // Check if this value already exists in TFIdentifier
        val existingConstant = identifierCache[identifierValue]
        if (existingConstant != null) {
            return "TFIdentifier.$existingConstant"
        }

        // Create a new constant name from the value
        val constantName = identifierValue.uppercase()

        // Add to TFIdentifier file
        addIdentifierToFile(project, constantName, identifierValue)

        // Update cache
        identifierCache[identifierValue] = constantName

        return "TFIdentifier.$constantName"
    }

    /**
     * Loads all existing identifiers from TFIdentifier.java file
     */
    private fun loadIdentifiersFromFile(project: Project) {
        try {
            val basePath = project.basePath ?: return
            val settings = PluginSettings.getInstance(project)
            val identifierFile = File(basePath, settings.tfIdentifierPath)

            if (!identifierFile.exists()) {
                cacheInitialized = true
                return
            }

            val content = identifierFile.readText()

            // Parse public static final String declarations
            // Pattern: public static final String IDENTIFIER_NAME = "value";
            val pattern = Regex("""public\s+static\s+final\s+String\s+(\w+)\s*=\s*"([^"]+)";""")

            pattern.findAll(content).forEach { matchResult ->
                val constantName = matchResult.groupValues[1]
                val value = matchResult.groupValues[2]
                identifierCache[value] = constantName
            }

            cacheInitialized = true
        } catch (e: Exception) {
            // If file doesn't exist or can't be read, continue without cache
            cacheInitialized = true
        }
    }

    /**
     * Adds a new identifier to TFIdentifier.java file
     */
    private fun addIdentifierToFile(project: Project, constantName: String, value: String) {
        try {
            val basePath = project.basePath ?: return
            val settings = PluginSettings.getInstance(project)
            val identifierFile = File(basePath, settings.tfIdentifierPath)

            if (!identifierFile.exists()) {
                return
            }

            var content = identifierFile.readText()

            if (content.contains("public static final String $constantName =")) {
                return
            }

            val lastBraceIndex = content.lastIndexOf("}")
            if (lastBraceIndex == -1) {
                return
            }

            val beforeBrace = content.substring(0, lastBraceIndex)
            val lastFieldMatch = Regex("""(\s+)public\s+static\s+final\s+String\s+\w+\s*=\s*"[^"]+";""")
                .findAll(beforeBrace)
                .lastOrNull()

            val indentation = lastFieldMatch?.groupValues?.get(1) ?: "    "
            val newLine = "${indentation}public static final String $constantName = \"$value\";\n"

            val updatedContent = content.substring(0, lastBraceIndex) + newLine + content.substring(lastBraceIndex)

            identifierFile.writeText(updatedContent)

            VirtualFileManager.getInstance().refreshWithoutFileWatcher(false)

        } catch (e: Exception) {

        }
    }

    /**
     * Clears the cache - useful for testing or when file is modified externally
     */
    fun clearCache() {
        identifierCache.clear()
        cacheInitialized = false
    }

    /**
     * Checks if a specific identifier value exists in TFIdentifier class
     */
    fun hasIdentifier(project: Project, identifierValue: String): Boolean {
        if (!cacheInitialized) {
            loadIdentifiersFromFile(project)
        }
        return identifierCache.containsKey(identifierValue)
    }

    /**
     * Gets the constant name for a value if it exists, null otherwise
     */
    fun getConstantName(project: Project, identifierValue: String): String? {
        if (!cacheInitialized) {
            loadIdentifiersFromFile(project)
        }
        return identifierCache[identifierValue]
    }
}
