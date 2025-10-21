package org.jetbrains.plugins.template.designer.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBUI
import org.jetbrains.plugins.designer.services.GeminiService
import java.awt.BorderLayout
import java.awt.Dialog
import java.awt.Dimension
import java.awt.Font
import javax.swing.*

class AIGenerationDialog(private val project: Project) : DialogWrapper(project) {

    private val promptArea = JBTextArea(6, 50)
    private var generatedJson: String? = null

    init {
        title = "AI Screen Generation"
        promptArea.lineWrap = true
        promptArea.wrapStyleWord = true
        promptArea.font = Font("SF Pro Display", Font.PLAIN, 13)
        promptArea.text = "Örnek: Para transferi için bir form ekranı oluştur. Gönderen hesap, alıcı IBAN, tutar ve devam butonu olsun."
        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout())
        panel.border = JBUI.Borders.empty(15)
        panel.preferredSize = Dimension(600, 250)

        // Title
        val titleLabel = JBLabel("Ekran için ne istediğinizi açıklayın:")
        titleLabel.font = Font("SF Pro Display", Font.BOLD, 14)
        panel.add(titleLabel, BorderLayout.NORTH)

        // Prompt area with scroll
        val scrollPane = JScrollPane(promptArea).apply {
            border = JBUI.Borders.empty(10, 0, 10, 0)
        }
        panel.add(scrollPane, BorderLayout.CENTER)

        // Example hint
        val hintLabel = JLabel("<html><i>AI, mevcut component'leri kullanarak ekranınızı otomatik oluşturacak.</i></html>")
        hintLabel.foreground = JBUI.CurrentTheme.ContextHelp.FOREGROUND
        panel.add(hintLabel, BorderLayout.SOUTH)

        return panel
    }

    override fun doOKAction() {
        val userPrompt = promptArea.text.trim()

        if (userPrompt.isBlank()) {
            JOptionPane.showMessageDialog(
                contentPane,
                "Lütfen bir açıklama girin",
                "Hata",
                JOptionPane.WARNING_MESSAGE
            )
            return
        }

        try {
            // Show loading indicator
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

            // Generate in background thread
            object : SwingWorker<String?, Void>() {
                override fun doInBackground(): String? {
                    val geminiService = GeminiService(project)
                    return geminiService.generateScreenComponents(userPrompt)
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
