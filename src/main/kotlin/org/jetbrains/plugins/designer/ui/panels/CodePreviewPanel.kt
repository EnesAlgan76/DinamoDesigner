package org.jetbrains.plugins.template.designer.ui

import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import java.awt.*
import javax.swing.*

class CodePreviewPanel(
    private val onGenerateCode: () -> Pair<String, String>  // (flowCode, screenActionBlock)
) : JPanel(BorderLayout()) {

    private val flowCodeTextArea = JTextArea().apply {
        font = Font("JetBrains Mono", Font.PLAIN, 11)
        isEditable = false
        lineWrap = true
        wrapStyleWord = true
        text = "// Generated flow code will appear here..."
        background = JBColor(Color(248, 249, 250), Color(30, 30, 30))
        foreground = JBColor(Color(60, 60, 60), Color(200, 200, 200))
        border = JBUI.Borders.empty(10)
    }

    private val screenActionTextArea = JTextArea().apply {
        font = Font("JetBrains Mono", Font.PLAIN, 11)
        isEditable = false
        lineWrap = true
        wrapStyleWord = true
        text = "// ScreenActionImpl block will appear here..."
        background = JBColor(Color(248, 249, 250), Color(30, 30, 30))
        foreground = JBColor(Color(60, 60, 60), Color(200, 200, 200))
        border = JBUI.Borders.empty(10)
    }

    private val tabbedPane = JTabbedPane().apply {
        addTab("Flow Code", JScrollPane(flowCodeTextArea).apply {
            border = JBUI.Borders.customLine(JBColor(Color(220, 220, 220), Color(60, 60, 60)), 1)
        })
        addTab("ScreenAction Block", JScrollPane(screenActionTextArea).apply {
            border = JBUI.Borders.customLine(JBColor(Color(220, 220, 220), Color(60, 60, 60)), 1)
        })
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

        add(tabbedPane, BorderLayout.CENTER)
    }

    fun updateCode(flowCode: String, screenActionBlock: String) {
        flowCodeTextArea.text = flowCode
        flowCodeTextArea.caretPosition = 0

        screenActionTextArea.text = screenActionBlock
        screenActionTextArea.caretPosition = 0
    }

    fun clearCode() {
        flowCodeTextArea.text = "// Generated flow code will appear here..."
        screenActionTextArea.text = "// ScreenActionImpl block will appear here..."
    }

    private fun createGenerateButton(): JButton {
        return ModernButton("Generate Code", ModernButtonStyle.SUCCESS) {
            val (flowCode, screenActionBlock) = onGenerateCode()
            updateCode(flowCode, screenActionBlock)
        }
    }

    private fun createCopyButton(): JButton {
        return ModernButton("Copy to Clipboard", ModernButtonStyle.SECONDARY) {
            val currentTab = tabbedPane.selectedIndex
            val textToCopy = if (currentTab == 0) {
                flowCodeTextArea.text
            } else {
                screenActionTextArea.text
            }
            val stringSelection = java.awt.datatransfer.StringSelection(textToCopy)
            Toolkit.getDefaultToolkit().systemClipboard.setContents(stringSelection, null)
        }
    }
}
