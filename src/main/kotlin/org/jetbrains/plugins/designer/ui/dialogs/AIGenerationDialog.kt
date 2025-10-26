package org.jetbrains.plugins.template.designer.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBUI
import org.jetbrains.plugins.designer.services.GeminiService
import java.awt.*
import javax.swing.*

class AIGenerationDialog(private val project: Project) : DialogWrapper(project) {

    private val promptArea = JBTextArea(6, 50)
    private var generatedJson: String? = null

    init {
        title = "AI Screen Generation"
        promptArea.lineWrap = true
        promptArea.wrapStyleWord = true
        promptArea.font = Font("SF Pro Display", Font.PLAIN, 14)
        promptArea.margin = Insets(10, 10, 10, 10)
        promptArea.background = JBColor(Color(249, 250, 251), Color(38, 38, 38))
        promptArea.foreground = JBColor(Color(17, 24, 39), Color(229, 231, 235))
        promptArea.caretColor = JBColor(Color(99, 102, 241), Color(139, 92, 246))
        promptArea.text = "e.g., Create a login screen with email, password fields and a submit button"

        promptArea.addFocusListener(object : java.awt.event.FocusAdapter() {
            override fun focusGained(e: java.awt.event.FocusEvent) {
                if (promptArea.text == "e.g., Create a login screen with email, password fields and a submit button") {
                    promptArea.text = ""
                    promptArea.foreground = JBColor(Color(17, 24, 39), Color(229, 231, 235))
                }
            }

            override fun focusLost(e: java.awt.event.FocusEvent) {
                if (promptArea.text.trim().isEmpty()) {
                    promptArea.text = "e.g., Create a login screen with email, password fields and a submit button"
                    promptArea.foreground = JBColor(Color(156, 163, 175), Color(107, 114, 128))
                }
            }
        })

        init()

        SwingUtilities.invokeLater {
            promptArea.requestFocusInWindow()
            promptArea.selectAll()
        }
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout())
        panel.border = JBUI.Borders.empty(20)
        panel.preferredSize = Dimension(650, 280)

        val titleLabel = JLabel("✨ Describe the screen you want to create").apply {
            font = Font("SF Pro Display", Font.BOLD, 15)
            foreground = JBColor(Color(71, 85, 105), Color(148, 163, 184))
            border = JBUI.Borders.emptyBottom(12)
        }

        val inputContainer = object : JPanel(BorderLayout()) {
            override fun paintComponent(g: Graphics) {
                val g2d = g as Graphics2D
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

                g2d.color = promptArea.background
                g2d.fillRoundRect(0, 0, width, height, 12, 12)

                g2d.color = JBColor(Color(203, 213, 225), Color(75, 85, 99))
                g2d.drawRoundRect(0, 0, width - 1, height - 1, 12, 12)

                if (promptArea.hasFocus()) {
                    g2d.color = JBColor(Color(99, 102, 241, 180), Color(139, 92, 246, 180))
                    g2d.setStroke(BasicStroke(2f))
                    g2d.drawRoundRect(0, 0, width - 1, height - 1, 12, 12)
                }

                super.paintComponent(g)
            }
        }.apply {
            isOpaque = false
            border = JBUI.Borders.empty(2)
        }

        promptArea.addFocusListener(object : java.awt.event.FocusAdapter() {
            override fun focusGained(e: java.awt.event.FocusEvent) {
                if (promptArea.text == "e.g., Create a login screen with email, password fields and a submit button") {
                    promptArea.text = ""
                    promptArea.foreground = JBColor(Color(17, 24, 39), Color(229, 231, 235))
                }
                inputContainer.repaint()
            }

            override fun focusLost(e: java.awt.event.FocusEvent) {
                if (promptArea.text.trim().isEmpty()) {
                    promptArea.text = "e.g., Create a login screen with email, password fields and a submit button"
                    promptArea.foreground = JBColor(Color(156, 163, 175), Color(107, 114, 128))
                }
                inputContainer.repaint()
            }
        })

        promptArea.foreground = JBColor(Color(156, 163, 175), Color(107, 114, 128))

        val scrollPane = JScrollPane(promptArea).apply {
            border = null
            isOpaque = false
            viewport.isOpaque = false
        }

        inputContainer.add(scrollPane, BorderLayout.CENTER)

        panel.add(titleLabel, BorderLayout.NORTH)
        panel.add(inputContainer, BorderLayout.CENTER)

        return panel
    }

    override fun doOKAction() {
        val userPrompt = promptArea.text.trim()

        if (userPrompt.isBlank() || userPrompt == "e.g., Create a login screen with email, password fields and a submit button") {
            JOptionPane.showMessageDialog(
                contentPane,
                "Please enter a screen description",
                "Empty Prompt",
                JOptionPane.WARNING_MESSAGE
            )
            return
        }

        generateScreen(userPrompt)
    }

    private fun generateScreen(prompt: String) {
        try {
            val progressDialog = JDialog(window, "Generating...", Dialog.ModalityType.MODELESS)
            val progressPanel = JPanel(BorderLayout())
            progressPanel.border = JBUI.Borders.empty(20)
            progressPanel.add(JLabel("AI ekran oluşturuyor..."), BorderLayout.CENTER)
            val progressBar = JProgressBar()
            progressBar.isIndeterminate = true
            progressPanel.add(progressBar, BorderLayout.SOUTH)
            progressDialog.contentPane = progressPanel
            progressDialog.setSize(300, 100)
            progressDialog.setLocationRelativeTo(window)

            object : SwingWorker<String?, Void>() {
                override fun doInBackground(): String? {
                    val geminiService = GeminiService(project)
                    return geminiService.generateScreenComponents(prompt)
                }

                override fun done() {
                    progressDialog.dispose()
                    try {
                        generatedJson = get()
                        if (generatedJson != null) {
                            super@AIGenerationDialog.doOKAction()
                        } else {
                            JOptionPane.showMessageDialog(
                                contentPane,
                                "AI yanıt üretemedi. Lütfen tekrar deneyin.",
                                "Hata",
                                JOptionPane.ERROR_MESSAGE
                            )
                        }
                    } catch (e: Exception) {
                        JOptionPane.showMessageDialog(
                            contentPane,
                            "Hata: ${e.message}",
                            "AI Generation Error",
                            JOptionPane.ERROR_MESSAGE
                        )
                    }
                }
            }.execute()

            progressDialog.isVisible = true

        } catch (e: Exception) {
            JOptionPane.showMessageDialog(
                contentPane,
                "Hata: ${e.message}",
                "Error",
                JOptionPane.ERROR_MESSAGE
            )
        }
    }

    fun getGeneratedJson(): String? = generatedJson
}
