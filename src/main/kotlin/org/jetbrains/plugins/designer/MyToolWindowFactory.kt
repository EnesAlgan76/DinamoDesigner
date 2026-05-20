package org.jetbrains.plugins.designer

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.content.ContentFactory
import com.intellij.util.ui.JBUI
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

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
                border = JBUI.Borders.empty(40, 40, 30, 40)
            }

            val centerPanel = JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                isOpaque = false

                val logoPanel = JPanel().apply {
                    layout = BoxLayout(this, BoxLayout.Y_AXIS)
                    isOpaque = false
                    alignmentX = Component.CENTER_ALIGNMENT

                    val iconLabel = JLabel("⚡").apply {
                        font = font.deriveFont(56f)
                        alignmentX = Component.CENTER_ALIGNMENT
                    }

                    val titleLabel = JBLabel("EA Suite").apply {
                        font = Font("SF Pro Display", Font.BOLD, 28)
                        foreground = JBColor(Color(30, 41, 59), Color(241, 245, 249))
                        alignmentX = Component.CENTER_ALIGNMENT
                    }

                    add(iconLabel)
                    add(Box.createVerticalStrut(12))
                    add(titleLabel)
                }

                add(logoPanel)
                add(Box.createVerticalStrut(40))

                val cardsPanel = JPanel(GridLayout(2, 2, 20, 20)).apply {
                    isOpaque = false
                    maximumSize = Dimension(700, 380)
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
                    "📋",
                    "Log Viewer",
                    "Gerçek zamanlı log takibi",
                    Color(139, 92, 246)
                ) {
                    openLogViewer()
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
                    "🔄",
                    "Backend Değiştir",
                    "Change numarası ve ortam ayarları",
                    Color(16, 185, 129)
                ) {
                    openBackendChanger()
                })

                add(cardsPanel)
            }

            mainPanel.add(centerPanel, BorderLayout.CENTER)
            toolWindowContent.add(mainPanel, BorderLayout.CENTER)
        }

        private fun openBackendChanger() {
            SwingUtilities.invokeLater {
                val dialog = org.jetbrains.plugins.designer.ui.dialogs.ChangeBackendDialog(project)
                dialog.show()
            }
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
                    preferredSize = Dimension(330, 180)

                    val contentPanel = JPanel().apply {
                        layout = BoxLayout(this, BoxLayout.Y_AXIS)
                        isOpaque = false
                        border = JBUI.Borders.empty(28, 24, 28, 24)

                        val emojiLabel = JLabel(emoji).apply {
                            font = font.deriveFont(52f)
                            alignmentX = Component.CENTER_ALIGNMENT
                        }

                        val titleLabel = JBLabel(title).apply {
                            font = Font("SF Pro Display", Font.BOLD, 20)
                            foreground = JBColor(Color(30, 41, 59), Color(241, 245, 249))
                            alignmentX = Component.CENTER_ALIGNMENT
                        }

                        val subtitleLabel = JBLabel(subtitle).apply {
                            font = Font("SF Pro Display", Font.PLAIN, 13)
                            foreground = JBColor(Color(100, 116, 139), Color(148, 163, 184))
                            alignmentX = Component.CENTER_ALIGNMENT
                        }

                        add(emojiLabel)
                        add(Box.createVerticalStrut(16))
                        add(titleLabel)
                        add(Box.createVerticalStrut(6))
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

        private fun openLogViewer() {
            SwingUtilities.invokeLater {
                val window = org.jetbrains.plugins.designer.ui.dialogs.LogViewerDialog(project)
                window.isVisible = true
            }
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