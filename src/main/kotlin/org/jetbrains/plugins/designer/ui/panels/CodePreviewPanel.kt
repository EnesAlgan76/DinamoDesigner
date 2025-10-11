package org.jetbrains.plugins.template.designer.ui

import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import java.awt.*
import javax.swing.*

class CodePreviewPanel(
    private val onGenerateCode: () -> String
) : JPanel(BorderLayout()) {

    private val codeTextArea = JTextArea().apply {
        font = Font("JetBrains Mono", Font.PLAIN, 11)
        isEditable = false
        lineWrap = true
        wrapStyleWord = true
        text = "// Generated code will appear here..."
        background = JBColor(Color(248, 249, 250), Color(30, 30, 30))
        foreground = JBColor(Color(60, 60, 60), Color(200, 200, 200))
        border = JBUI.Borders.empty(10)
    }

    init {
        border = JBUI.Borders.empty(20, 0, 0, 0)
        preferredSize = Dimension(320, 350)

        val buttonPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            isOpaque = false

            val generateButton = createGenerateButton()
            generateButton.maximumSize = Dimension(Integer.MAX_VALUE, 40)
            add(generateButton)
            add(Box.createVerticalStrut(10))
        }
        add(buttonPanel, BorderLayout.NORTH)

        val scrollPane = JScrollPane(codeTextArea).apply {
            border = JBUI.Borders.customLine(JBColor(Color(220, 220, 220), Color(60, 60, 60)), 1)
        }
        add(scrollPane, BorderLayout.CENTER)
    }

    fun updateCode(code: String) {
        codeTextArea.text = code
        codeTextArea.caretPosition = 0
    }

    fun clearCode() {
        codeTextArea.text = "// Generated code will appear here..."
    }

    private fun createGenerateButton(): JButton {
        return ModernButton("Generate Code", ModernButtonStyle.SUCCESS) {
            val code = onGenerateCode()
            updateCode(code)
        }
    }

    private fun createCopyButton(): JButton {
        return ModernButton("Copy to Clipboard", ModernButtonStyle.SECONDARY) {
            val stringSelection = java.awt.datatransfer.StringSelection(codeTextArea.text)
            Toolkit.getDefaultToolkit().systemClipboard.setContents(stringSelection, null)
        }
    }
}
