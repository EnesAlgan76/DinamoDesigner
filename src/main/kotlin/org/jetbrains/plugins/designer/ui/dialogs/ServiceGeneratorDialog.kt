package org.jetbrains.plugins.designer.ui.dialogs

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import org.jetbrains.plugins.designer.services.ServiceModelGenerator
import java.awt.*
import javax.swing.*

class ServiceGeneratorDialog(private val project: Project) : DialogWrapper(project) {

    private val serviceNameField = JBTextField(30)
    private val requestJsonArea = JBTextArea(15, 60)
    private val responseJsonArea = JBTextArea(15, 60)
    private val statusLabel = JBLabel("")

    init {
        title = "Service Generator"
        init()
    }

    override fun createCenterPanel(): JComponent {
        val mainPanel = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(20)
            preferredSize = Dimension(900, 700)
        }

        // Header
        val headerPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = JBUI.Borders.emptyBottom(20)

            add(JBLabel("🔧 REST Service Generator").apply {
                font = font.deriveFont(Font.BOLD, 20f)
                foreground = JBColor(Color(99, 102, 241), Color(167, 139, 250))
            })
            add(Box.createVerticalStrut(8))
            add(JBLabel("Generate service methods and model classes from JSON").apply {
                foreground = JBColor.GRAY
            })
        }


        val formPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)

            add(createFormField("Service Name (e.g., GetUserProfile):", serviceNameField))
            add(Box.createVerticalStrut(15))

            add(JBLabel("Request JSON (leave empty for no request):").apply {
                font = font.deriveFont(Font.BOLD)
                foreground = JBColor(Color(71, 85, 105), Color(203, 213, 225))
            })
            add(Box.createVerticalStrut(8))
            requestJsonArea.apply {
                lineWrap = true
                wrapStyleWord = true
                font = Font("Monospaced", Font.PLAIN, 12)
                text = """
{
  "accountNumber": "string",
  "amount": 100.50,
  "currency": "TRY"
}
                """.trimIndent()
            }
            add(JBScrollPane(requestJsonArea).apply {
                preferredSize = Dimension(800, 200)
            })
            add(Box.createVerticalStrut(15))

            add(JBLabel("Response JSON:").apply {
                font = font.deriveFont(Font.BOLD)
                foreground = JBColor(Color(71, 85, 105), Color(203, 213, 225))
            })
            add(Box.createVerticalStrut(8))
            responseJsonArea.apply {
                lineWrap = true
                wrapStyleWord = true
                font = Font("Monospaced", Font.PLAIN, 12)
                text = """
{
  "GetUserProfileResult": {
    "Result": {
      "name": "string",
      "email": "string",
      "balance": 0.0
    },
    "MobileValidationResult": {}
  }
}
                """.trimIndent()
            }
            add(JBScrollPane(responseJsonArea).apply {
                preferredSize = Dimension(800, 200)
            })
            add(Box.createVerticalStrut(15))

            // Status
            add(statusLabel.apply {
                foreground = JBColor.BLUE
            })
        }

        val scrollPane = JBScrollPane(formPanel).apply {
            border = null
        }

        mainPanel.add(headerPanel, BorderLayout.NORTH)
        mainPanel.add(scrollPane, BorderLayout.CENTER)

        return mainPanel
    }

    private fun createFormField(labelText: String, field: JTextField): JComponent {
        return JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            alignmentX = Component.LEFT_ALIGNMENT

            add(JBLabel(labelText).apply {
                font = font.deriveFont(Font.BOLD)
                foreground = JBColor(Color(71, 85, 105), Color(203, 213, 225))
            })
            add(Box.createVerticalStrut(8))
            add(field.apply {
                maximumSize = Dimension(Int.MAX_VALUE, preferredSize.height)
            })
        }
    }

    override fun doOKAction() {
        val serviceName = serviceNameField.text.trim()
        val requestJson = requestJsonArea.text.trim()
        val responseJson = responseJsonArea.text.trim()

        if (serviceName.isEmpty()) {
            JOptionPane.showMessageDialog(
                contentPane,
                "Please enter a service name",
                "Validation Error",
                JOptionPane.ERROR_MESSAGE
            )
            return
        }

        if (responseJson.isEmpty()) {
            JOptionPane.showMessageDialog(
                contentPane,
                "Please enter response JSON",
                "Validation Error",
                JOptionPane.ERROR_MESSAGE
            )
            return
        }

        try {
            statusLabel.text = "⏳ Generating service files..."
            statusLabel.foreground = JBColor.BLUE

            val generator = ServiceModelGenerator(project)
            val result = generator.generateService(
                serviceName = serviceName,
                requestJson = if (requestJson.isEmpty()) null else requestJson,
                responseJson = responseJson
            )

            statusLabel.text = "✅ Success! Generated ${result.filesCreated} files"
            statusLabel.foreground = JBColor(Color(34, 197, 94), Color(74, 222, 128))

            // Show success dialog with details
            val message = buildString {
                appendLine("Service generated successfully!")
                appendLine()
                appendLine("Files created:")
                result.createdFiles.forEach { appendLine("  • $it") }
                appendLine()
                appendLine("You can now call:")
                appendLine("  adcRestClient.$serviceName(...)")
            }

            JOptionPane.showMessageDialog(
                contentPane,
                message,
                "Success",
                JOptionPane.INFORMATION_MESSAGE
            )

            super.doOKAction()

        } catch (e: Exception) {
            statusLabel.text = "❌ Error: ${e.message}"
            statusLabel.foreground = JBColor.RED

            JOptionPane.showMessageDialog(
                contentPane,
                "Failed to generate service:\n${e.message}",
                "Error",
                JOptionPane.ERROR_MESSAGE
            )
        }
    }

    override fun createActions(): Array<Action> {
        return arrayOf(okAction, cancelAction)
    }
}
