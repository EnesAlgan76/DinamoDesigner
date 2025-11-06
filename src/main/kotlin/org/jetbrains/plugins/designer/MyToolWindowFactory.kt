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
        val mainMenu = MainMenuWindow(project)
        val content = ContentFactory.getInstance().createContent(mainMenu.getContent(), "", false)
        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project) = true


    class MainMenuWindow(private val project: Project) {

        private val toolWindowContent: JPanel

        init {
            toolWindowContent = JBPanel<JBPanel<*>>(BorderLayout()).apply {
                background = JBColor(Color(250, 251, 252), Color(25, 27, 31))
            }

            createUI()
        }

        private fun createUI() {
            val mainPanel = JPanel(BorderLayout()).apply {
                isOpaque = false
                border = JBUI.Borders.empty(60, 40, 40, 40)
            }

            val centerPanel = JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                isOpaque = false

                val logoPanel = JPanel().apply {
                    layout = BoxLayout(this, BoxLayout.Y_AXIS)
                    isOpaque = false
                    alignmentX = Component.CENTER_ALIGNMENT

                    val iconLabel = JLabel("⚡").apply {
                        font = font.deriveFont(72f)
                        alignmentX = Component.CENTER_ALIGNMENT
                    }

                    val titleLabel = JBLabel("EA Suite").apply {
                        font = Font("SF Pro Display", Font.BOLD, 32)
                        foreground = JBColor(Color(30, 41, 59), Color(241, 245, 249))
                        alignmentX = Component.CENTER_ALIGNMENT
                    }

                    add(iconLabel)
                    add(Box.createVerticalStrut(16))
                    add(titleLabel)
                }

                add(logoPanel)
                add(Box.createVerticalStrut(60))

                val cardsPanel = JPanel(GridLayout(2, 2, 24, 24)).apply {
                    isOpaque = false
                    maximumSize = Dimension(800, 500)
                }

                cardsPanel.add(createFeatureCard(
                    "🎨",
                    "Dinamo Designer",
                    "Visual screen designer",
                    Color(99, 102, 241)
                ) {
                    openDesigner()
                })

                cardsPanel.add(createFeatureCard(
                    "📊",
                    "If Counter",
                    "Analyze code complexity",
                    Color(139, 92, 246)
                ) {
                    openIfCounter()
                })

                cardsPanel.add(createFeatureCard(
                    "🔧",
                    "Service Generator",
                    "Generate REST services from JSON",
                    Color(236, 72, 153)
                ) {
                    openServiceGenerator()
                })

                cardsPanel.add(createFeatureCard(
                    "✨",
                    "Coming Soon",
                    "Stay tuned",
                    Color(148, 163, 184)
                ) {
                })

                add(cardsPanel)
            }

            mainPanel.add(centerPanel, BorderLayout.CENTER)
            toolWindowContent.add(mainPanel, BorderLayout.CENTER)
        }

        private fun createFeatureCard(emoji: String, title: String, subtitle: String, accentColor: Color, action: () -> Unit): JPanel {
            return object : JPanel() {
                private var isHovered = false
                private var hoverProgress = 0f
                private val animationTimer = Timer(16) {
                    hoverProgress = if (isHovered) {
                        minOf(hoverProgress + 0.15f, 1f)
                    } else {
                        maxOf(hoverProgress - 0.15f, 0f)
                    }
                    if ((isHovered && hoverProgress >= 1f) || (!isHovered && hoverProgress <= 0f)) {
                        (it.source as Timer).stop()
                    }
                    repaint()
                }

                init {
                    layout = BorderLayout()
                    isOpaque = false
                    cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                    preferredSize = Dimension(360, 220)

                    val contentPanel = JPanel().apply {
                        layout = BoxLayout(this, BoxLayout.Y_AXIS)
                        isOpaque = false
                        border = JBUI.Borders.empty(32)

                        val emojiLabel = JLabel(emoji).apply {
                            font = font.deriveFont(56f)
                            alignmentX = Component.CENTER_ALIGNMENT
                        }

                        val titleLabel = JBLabel(title).apply {
                            font = Font("SF Pro Display", Font.BOLD, 22)
                            foreground = JBColor(Color(30, 41, 59), Color(241, 245, 249))
                            alignmentX = Component.CENTER_ALIGNMENT
                        }

                        val subtitleLabel = JBLabel(subtitle).apply {
                            font = Font("SF Pro Display", Font.PLAIN, 14)
                            foreground = JBColor(Color(100, 116, 139), Color(148, 163, 184))
                            alignmentX = Component.CENTER_ALIGNMENT
                        }

                        add(emojiLabel)
                        add(Box.createVerticalStrut(20))
                        add(titleLabel)
                        add(Box.createVerticalStrut(8))
                        add(subtitleLabel)
                    }

                    add(contentPanel, BorderLayout.CENTER)

                    addMouseListener(object : MouseAdapter() {
                        override fun mouseClicked(e: MouseEvent) {
                            action()
                        }

                        override fun mouseEntered(e: MouseEvent) {
                            isHovered = true
                            animationTimer.start()
                        }

                        override fun mouseExited(e: MouseEvent) {
                            isHovered = false
                            animationTimer.start()
                        }
                    })
                }

                override fun paintComponent(g: Graphics) {
                    val g2d = g as Graphics2D
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

                    val shadowAlpha = (15 + hoverProgress * 25).toInt()
                    val shadowOffset = (4 + hoverProgress * 8).toInt()
                    g2d.color = Color(0, 0, 0, shadowAlpha)
                    g2d.fillRoundRect(shadowOffset, shadowOffset, width - shadowOffset, height - shadowOffset, 20, 20)

                    val bgColor = if (accentColor == Color(148, 163, 184)) {
                        JBColor(Color(255, 255, 255, 200), Color(40, 44, 52, 200))
                    } else {
                        JBColor(Color(255, 255, 255, 240), Color(40, 44, 52, 240))
                    }
                    g2d.color = bgColor
                    g2d.fillRoundRect(0, 0, width, height, 20, 20)

                    val borderAlpha = (100 + hoverProgress * 155).toInt()
                    g2d.color = Color(accentColor.red, accentColor.green, accentColor.blue, borderAlpha)
                    val strokeWidth = 2f + hoverProgress * 1f
                    g2d.setStroke(BasicStroke(strokeWidth))
                    g2d.drawRoundRect(1, 1, width - 3, height - 3, 20, 20)

                    if (hoverProgress > 0) {
                        val overlayAlpha = (hoverProgress * 40).toInt()
                        g2d.color = Color(accentColor.red, accentColor.green, accentColor.blue, overlayAlpha)
                        g2d.fillRoundRect(0, 0, width, height, 20, 20)
                    }

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

        private fun openIfCounter() {
            JOptionPane.showMessageDialog(
                toolWindowContent,
                "If Counter feature - Coming soon!",
                "Info",
                JOptionPane.INFORMATION_MESSAGE
            )
        }

        private fun openServiceGenerator() {
            SwingUtilities.invokeLater {
                val dialog = org.jetbrains.plugins.designer.ui.dialogs.ServiceGeneratorDialog(project)
                dialog.show()
            }
        }

        fun getContent(): JComponent = toolWindowContent
    }
}