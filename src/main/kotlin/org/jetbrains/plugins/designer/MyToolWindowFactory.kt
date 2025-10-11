package org.jetbrains.plugins.designer

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentFactory
import com.intellij.util.ui.JBUI
import org.jetbrains.plugins.template.MyBundle
import org.jetbrains.plugins.template.services.IfStatementInfo
import org.jetbrains.plugins.template.services.MyProjectService
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.border.EmptyBorder

class MyToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val myToolWindow = MyToolWindow(project)
        val content = ContentFactory.getInstance().createContent(myToolWindow.getContent(), "If Analyzer", false)
        toolWindow.contentManager.addContent(content)

        // Add Dinamo Designer tab
        val dinamoWindow = DinamoToolWindow(project)
        val dinamoContent = ContentFactory.getInstance().createContent(dinamoWindow.getContent(), "Dinamo Designer", false)
        toolWindow.contentManager.addContent(dinamoContent)
    }

    override fun shouldBeAvailable(project: Project) = true

    // Original If Analyzer Window
    class MyToolWindow(private val project: Project) : MyProjectService.IfCounterListener {

        private val myToolWindowContent: JPanel
        private lateinit var countLabel: JBLabel
        private lateinit var fileNameLabel: JBLabel
        private lateinit var statusLabel: JBLabel
        private lateinit var jsonTextArea: JTextArea
        private lateinit var scrollPane: JBScrollPane
        private val service = project.service<MyProjectService>()

        init {
            myToolWindowContent = JBPanel<JBPanel<*>>(BorderLayout()).apply {
                border = EmptyBorder(JBUI.insets(10))
                background = JBColor.PanelBackground
            }

            createUI()
            service.addListener(this)
            service.checkCurrentFile()
        }

        private fun createUI() {
            val headerPanel = createHeaderPanel()
            val contentPanel = createContentPanel()

            myToolWindowContent.add(headerPanel, BorderLayout.NORTH)
            myToolWindowContent.add(contentPanel, BorderLayout.CENTER)
        }

        private fun createHeaderPanel(): JPanel {
            return JPanel(BorderLayout()).apply {
                isOpaque = false
                border = JBUI.Borders.empty(0, 0, 15, 0)

                val titleLabel = JBLabel("If Statement Analyzer").apply {
                    font = font.deriveFont(Font.BOLD, 16f)
                    foreground = JBColor.foreground()
                }

                val iconLabel = JBLabel("📊").apply {
                    font = font.deriveFont(20f)
                }

                add(iconLabel, BorderLayout.WEST)
                add(titleLabel, BorderLayout.CENTER)
                add(JSeparator(), BorderLayout.SOUTH)
            }
        }

        private fun createContentPanel(): JPanel {
            return JPanel(BorderLayout()).apply {
                isOpaque = false
                border = JBUI.Borders.empty(10)

                val topPanel = createTopInfoPanel()
                add(topPanel, BorderLayout.NORTH)

                jsonTextArea = JTextArea().apply {
                    font = Font("JetBrains Mono", Font.PLAIN, 12)
                    isEditable = false
                    background = JBColor.PanelBackground
                    foreground = JBColor.foreground()
                    lineWrap = false
                    wrapStyleWord = false
                    text = "// Open a Java file to see if-else structure"
                }

                scrollPane = JBScrollPane(jsonTextArea).apply {
                    border = JBUI.Borders.customLine(JBColor.border(), 1)
                    background = JBColor.PanelBackground
                    viewport.background = JBColor.PanelBackground
                    verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
                    horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
                }

                add(scrollPane, BorderLayout.CENTER)

                val bottomPanel = createBottomPanel()
                add(bottomPanel, BorderLayout.SOUTH)
            }
        }

        private fun createTopInfoPanel(): JPanel {
            return JPanel(BorderLayout()).apply {
                isOpaque = false
                border = JBUI.Borders.empty(0, 0, 10, 0)

                fileNameLabel = JBLabel(MyBundle.message("noFile")).apply {
                    font = font.deriveFont(Font.BOLD, 14f)
                    foreground = JBColor.foreground()
                }

                val countPanel = JPanel(FlowLayout(FlowLayout.CENTER)).apply {
                    isOpaque = false

                    countLabel = JBLabel("0").apply {
                        font = font.deriveFont(Font.BOLD, 32f)
                        foreground = JBColor.GRAY
                    }

                    val countText = JBLabel("if statements").apply {
                        font = font.deriveFont(Font.PLAIN, 12f)
                        foreground = JBColor.GRAY
                    }

                    add(countLabel)
                    add(Box.createHorizontalStrut(10))
                    add(countText)
                }

                statusLabel = JBLabel(MyBundle.message("openJavaFile")).apply {
                    foreground = JBColor.GRAY
                    horizontalAlignment = SwingConstants.CENTER
                    font = font.deriveFont(Font.ITALIC, 12f)
                }

                add(fileNameLabel, BorderLayout.NORTH)
                add(countPanel, BorderLayout.CENTER)
                add(statusLabel, BorderLayout.SOUTH)
            }
        }

        private fun createBottomPanel(): JPanel {
            return JPanel(FlowLayout()).apply {
                isOpaque = false
                border = JBUI.Borders.empty(10, 0, 0, 0)

                val refreshButton = JButton(MyBundle.message("refresh")).apply {
                    addActionListener {
                        service.checkCurrentFile()
                    }
                }

                add(refreshButton)
            }
        }

        override fun onIfStatementsChanged(statements: List<IfStatementInfo>, fileName: String) {
            SwingUtilities.invokeLater {
                countLabel.text = statements.size.toString()

                if (fileName.isEmpty()) {
                    fileNameLabel.text = MyBundle.message("noFile")
                    statusLabel.text = MyBundle.message("openJavaFile")
                    countLabel.foreground = JBColor.GRAY
                    jsonTextArea.text = "// Open a Java file to see if-else structure"
                } else {
                    fileNameLabel.text = MyBundle.message("fileName", fileName)

                    val maxNesting = statements.maxOfOrNull { it.nestingLevel } ?: 0
                    val nestedCount = statements.count { it.nestingLevel > 0 }
                    val elseCount = statements.count { it.isElse }

                    when (statements.size) {
                        0 -> {
                            statusLabel.text = MyBundle.message("noIfs")
                            countLabel.foreground = JBColor.GRAY
                            jsonTextArea.text = "// $fileName\n\nNo if statements found"
                        }
                        1 -> {
                            statusLabel.text = MyBundle.message("oneIf")
                            countLabel.foreground = JBColor(Color(76, 175, 80), Color(76, 175, 80))
                            jsonTextArea.text = service.generateJsonStructure()
                        }
                        else -> {
                            val statusText = buildString {
                                append("${statements.size} blocks")
                                if (elseCount > 0) {
                                    append(" (${elseCount} else)")
                                }
                                if (nestedCount > 0) {
                                    append(" (${nestedCount} nested, max depth: ${maxNesting})")
                                }
                            }
                            statusLabel.text = statusText
                            countLabel.foreground = JBColor(Color(76, 175, 80), Color(76, 175, 80))
                            jsonTextArea.text = service.generateJsonStructure()
                        }
                    }
                }

                jsonTextArea.caretPosition = 0
            }
        }

        fun getContent(): JComponent = myToolWindowContent
    }

    // New Dinamo Designer Window
    class DinamoToolWindow(private val project: Project) {

        private val toolWindowContent: JPanel

        init {
            toolWindowContent = JBPanel<JBPanel<*>>(BorderLayout()).apply {
                border = EmptyBorder(JBUI.insets(20))
                background = JBColor.PanelBackground
            }

            createUI()
        }

        private fun createUI() {
            val mainPanel = JPanel(BorderLayout()).apply {
                isOpaque = false
            }

            // Header
            val headerPanel = createHeaderPanel()
            mainPanel.add(headerPanel, BorderLayout.NORTH)

            // Content
            val contentPanel = createContentPanel()
            mainPanel.add(contentPanel, BorderLayout.CENTER)

            toolWindowContent.add(mainPanel, BorderLayout.CENTER)
        }

        private fun createHeaderPanel(): JPanel {
            return JPanel(BorderLayout()).apply {
                isOpaque = false
                border = JBUI.Borders.empty(0, 0, 20, 0)

                val titlePanel = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
                    isOpaque = false

                    val iconLabel = JLabel("🎨").apply {
                        font = font.deriveFont(24f)
                    }

                    val titleLabel = JBLabel("Dinamo Multi-Screen Designer").apply {
                        font = font.deriveFont(Font.BOLD, 18f)
                        foreground = JBColor.foreground()
                    }

                    add(iconLabel)
                    add(Box.createHorizontalStrut(10))
                    add(titleLabel)
                }

                add(titlePanel, BorderLayout.WEST)
                add(JSeparator(), BorderLayout.SOUTH)
            }
        }

        private fun createContentPanel(): JPanel {
            return JPanel(BorderLayout()).apply {
                isOpaque = false

                // Welcome message
                val welcomePanel = JPanel().apply {
                    layout = BoxLayout(this, BoxLayout.Y_AXIS)
                    isOpaque = false
                    border = JBUI.Borders.empty(40)

                    val iconLabel = JLabel("📱").apply {
                        font = font.deriveFont(80f)
                        alignmentX = Component.CENTER_ALIGNMENT
                    }

                    val titleLabel = JBLabel("Welcome to Dinamo Designer").apply {
                        font = font.deriveFont(Font.BOLD, 24f)
                        foreground = JBColor.foreground()
                        alignmentX = Component.CENTER_ALIGNMENT
                    }

                    val descLabel = JBLabel("<html><center>Design mobile screens visually and generate Java code<br>for your Dinamo components</center></html>").apply {
                        font = font.deriveFont(Font.PLAIN, 14f)
                        foreground = JBColor.GRAY
                        alignmentX = Component.CENTER_ALIGNMENT
                    }

                    add(iconLabel)
                    add(Box.createVerticalStrut(20))
                    add(titleLabel)
                    add(Box.createVerticalStrut(10))
                    add(descLabel)
                }

                add(welcomePanel, BorderLayout.CENTER)

                // Open Designer Button
                val buttonPanel = JPanel(FlowLayout(FlowLayout.CENTER)).apply {
                    isOpaque = false
                    border = JBUI.Borders.empty(20, 0, 0, 0)

                    val openDesignerButton = createStyledButton(
                        "🚀 Open Designer",
                        JBColor(Color(0, 123, 255), Color(0, 123, 255))
                    ) {
                        openDesigner()
                    }


                    add(openDesignerButton)
                    add(Box.createHorizontalStrut(15))
                }

                add(buttonPanel, BorderLayout.SOUTH)

                // Features list
                val featuresPanel = createFeaturesPanel()
                add(featuresPanel, BorderLayout.NORTH)
            }
        }

        private fun createFeaturesPanel(): JPanel {
            return JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                isOpaque = false
                border = JBUI.Borders.empty(20, 40, 20, 40)

                val featuresTitle = JBLabel("Features:").apply {
                    font = font.deriveFont(Font.BOLD, 14f)
                    foreground = JBColor.foreground()
                    alignmentX = Component.LEFT_ALIGNMENT
                }

                add(featuresTitle)
                add(Box.createVerticalStrut(15))

                val features = listOf(
                    "✨ Drag & Drop component library",
                    "📱 Multi-screen management",
                    "🔗 Navigation flow between screens",
                    "⚙️ Component properties editor",
                    "💻 Java code generation",
                    "🎯 Visual screen flow diagram"
                )

                features.forEach { feature ->
                    val featureLabel = JBLabel(feature).apply {
                        font = font.deriveFont(Font.PLAIN, 13f)
                        foreground = JBColor.GRAY
                        alignmentX = Component.LEFT_ALIGNMENT
                    }
                    add(featureLabel)
                    add(Box.createVerticalStrut(8))
                }
            }
        }

        private fun createStyledButton(text: String, bgColor: JBColor, action: () -> Unit): JButton {
            return object : JButton(text) {
                init {
                    font = font.deriveFont(Font.BOLD, 14f)
                    foreground = Color.WHITE
                    background = bgColor
                    isFocusPainted = false
                    preferredSize = Dimension(200, 45)
                    cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)

                    addActionListener { action() }

                    addMouseListener(object : MouseAdapter() {
                        override fun mouseEntered(e: MouseEvent) {
                            background = bgColor.brighter()
                        }

                        override fun mouseExited(e: MouseEvent) {
                            background = bgColor
                        }
                    })
                }

                override fun paintComponent(g: Graphics) {
                    val g2d = g as Graphics2D
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

                    // Shadow
                    g2d.color = Color(0, 0, 0, 30)
                    g2d.fillRoundRect(2, 2, width - 2, height - 2, 12, 12)

                    // Button background
                    g2d.color = background
                    g2d.fillRoundRect(0, 0, width, height, 12, 12)

                    // Border
                    g2d.color = Color(255, 255, 255, 80)
                    g2d.drawRoundRect(0, 0, width - 1, height - 1, 12, 12)

                    super.paintComponent(g)
                }
            }
        }

        private fun openDesigner() {
            SwingUtilities.invokeLater {
                val dialog = DinamoDesignerDialog(project)
                dialog.isVisible = true
            }
        }

        fun getContent(): JComponent = toolWindowContent
    }
}