package org.jetbrains.plugins.template.designer

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import org.jetbrains.plugins.designer.models.Screen
import java.awt.Component
import java.io.File
import javax.swing.JFileChooser
import javax.swing.JOptionPane
import javax.swing.filechooser.FileNameExtensionFilter

object DesignPersistence {

    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .create()

    private const val FILE_EXTENSION = "dinamo"
    private const val FILE_DESCRIPTION = "Dinamo Design Files (*.dinamo)"

    // ========== EXPORT ==========

    fun exportDesign(screens: List<Screen>, parentComponent: Component?) {
        if (screens.isEmpty()) {
            showErrorDialog("No screens to export", parentComponent)
            return
        }

        try {
            val filePath = showSaveDialog(parentComponent) ?: return
            val json = serializeToJson(screens)
            val success = writeToFile(filePath, json)

            if (success) {
                showSuccessDialog("Design exported successfully to:\n$filePath", parentComponent)
            } else {
                showErrorDialog("Failed to export design", parentComponent)
            }
        } catch (e: Exception) {
            showErrorDialog("Export error: ${e.message}", parentComponent)
        }
    }

    private fun serializeToJson(screens: List<Screen>): String {
        return gson.toJson(screens)
    }

    private fun showSaveDialog(parentComponent: Component?): String? {
        val fileChooser = JFileChooser().apply {
            dialogTitle = "Export Design"
            fileFilter = FileNameExtensionFilter(FILE_DESCRIPTION, FILE_EXTENSION)
            selectedFile = File("dinamo_design.$FILE_EXTENSION")
        }

        val result = fileChooser.showSaveDialog(parentComponent)

        if (result == JFileChooser.APPROVE_OPTION) {
            var file = fileChooser.selectedFile

            // Add extension if not present
            if (!file.name.endsWith(".$FILE_EXTENSION")) {
                file = File(file.absolutePath + ".$FILE_EXTENSION")
            }

            // Confirm overwrite if file exists
            if (file.exists()) {
                val overwrite = JOptionPane.showConfirmDialog(
                    parentComponent,
                    "File already exists. Overwrite?",
                    "Confirm Overwrite",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
                )

                if (overwrite != JOptionPane.YES_OPTION) {
                    return null
                }
            }

            return file.absolutePath
        }

        return null
    }

    private fun writeToFile(filePath: String, content: String): Boolean {
        return try {
            File(filePath).writeText(content)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

 

    fun importDesign(parentComponent: Component?): List<Screen>? {
        try {
            val filePath = showOpenDialog(parentComponent) ?: return null
            val json = readFromFile(filePath) ?: run {
                showErrorDialog("Failed to read file", parentComponent)
                return null
            }

            val screens = deserializeFromJson(json)

            if (screens == null) {
                showErrorDialog("Invalid design file format", parentComponent)
                return null
            }

            if (!validateDesignData(screens)) {
                showErrorDialog("Design file validation failed", parentComponent)
                return null
            }

            return screens

        } catch (e: Exception) {
            showErrorDialog("Import error: ${e.message}", parentComponent)
            return null
        }
    }

    private fun showOpenDialog(parentComponent: Component?): String? {
        val fileChooser = JFileChooser().apply {
            dialogTitle = "Import Design"
            fileFilter = FileNameExtensionFilter(FILE_DESCRIPTION, FILE_EXTENSION)
        }

        val result = fileChooser.showOpenDialog(parentComponent)

        return if (result == JFileChooser.APPROVE_OPTION) {
            fileChooser.selectedFile.absolutePath
        } else {
            null
        }
    }

    private fun readFromFile(filePath: String): String? {
        return try {
            File(filePath).readText()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun deserializeFromJson(json: String): List<Screen>? {
        return try {
            val type = object : TypeToken<List<Screen>>() {}.type
            gson.fromJson<List<Screen>>(json, type)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun validateDesignData(screens: List<Screen>): Boolean {
        if (screens.isEmpty()) return false

        // Check for duplicate screen IDs
        val ids = screens.map { it.id }
        if (ids.size != ids.distinct().size) return false

        // Check for duplicate screen names
        val names = screens.map { it.name }
        if (names.size != names.distinct().size) return false

        // Validate each screen
        screens.forEach { screen ->
            if (screen.id.isBlank()) return false
            if (screen.name.isBlank()) return false

            // Check for duplicate component IDs within screen
            val componentIds = screen.components.map { it.id }
            if (componentIds.size != componentIds.distinct().size) return false

            // Validate each component
            screen.components.forEach { component ->
                if (component.id.isBlank()) return false
                if (component.type.isBlank()) return false
            }
        }

        return true
    }

    // ========== DIALOGS ==========

    private fun showErrorDialog(message: String, parentComponent: Component?) {
        JOptionPane.showMessageDialog(
            parentComponent,
            message,
            "Error",
            JOptionPane.ERROR_MESSAGE
        )
    }

    private fun showSuccessDialog(message: String, parentComponent: Component?) {
        JOptionPane.showMessageDialog(
            parentComponent,
            message,
            "Success",
            JOptionPane.INFORMATION_MESSAGE
        )
    }
}

// ========== JSON SERIALIZATION HELPERS ==========

internal object JsonSerializer {

    private val gson = GsonBuilder().setPrettyPrinting().create()

    fun screenToJson(screen: Screen): String {
        return gson.toJson(screen)
    }

    fun screensToJson(screens: List<Screen>): String {
        return gson.toJson(screens)
    }

    fun jsonToScreen(json: String): Screen? {
        return try {
            gson.fromJson(json, Screen::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun jsonToScreens(json: String): List<Screen>? {
        return try {
            val type = object : TypeToken<List<Screen>>() {}.type
            gson.fromJson<List<Screen>>(json, type)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}