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
                border = JBUI.Borders.empty(40, 40, 30, 40)
            }

            // Small backend config in top right corner
            val topRightPanel = createBackendConfigPanel()
            mainPanel.add(topRightPanel, BorderLayout.NORTH)

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

        private fun createBackendConfigPanel(): JPanel {
            return JPanel(FlowLayout(FlowLayout.RIGHT, 8, 8)).apply {
                isOpaque = false
                maximumSize = Dimension(600, 40)

                add(JBLabel("⚙️").apply {
                    font = font.deriveFont(16f)
                    toolTipText = "Backend Change"
                })

                val changeNoField = JTextField(8).apply {
                    toolTipText = "Change no (örn: 144252)"
                    font = Font("SF Pro Display", Font.PLAIN, 12)
                    background = JBColor(Color(255, 255, 255), Color(45, 48, 54))
                    foreground = JBColor(Color(71, 85, 105), Color(203, 213, 225))
                    border = BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(JBColor(Color(203, 213, 225), Color(71, 85, 105)), 1),
                        JBUI.Borders.empty(4, 8)
                    )
                }
                add(changeNoField)

                add(JButton("✓").apply {
                    font = Font("SF Pro Display", Font.BOLD, 16)
                    foreground = Color.WHITE
                    background = Color(34, 197, 94)
                    preferredSize = Dimension(32, 28)
                    toolTipText = "Uygula"
                    isOpaque = true
                    isBorderPainted = false
                    cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                    isFocusPainted = false

                    addMouseListener(object : MouseAdapter() {
                        override fun mouseEntered(e: MouseEvent) {
                            background = Color(22, 163, 74)
                        }
                        override fun mouseExited(e: MouseEvent) {
                            background = Color(34, 197, 94)
                        }
                    })

                    addActionListener {
                        val defaultPath = "/Users/enesalgan/Projeler/DinamoDesigner/src/main/kotlin/org/jetbrains/plugins/turkiyefinans.properties"
                        applyBackendChanges(defaultPath, changeNoField.text)
                    }
                })
            }
        }

        private fun applyBackendChanges(filePath: String, changeNo: String) {
            if (filePath.isBlank() || changeNo.isBlank()) {
                JOptionPane.showMessageDialog(
                    toolWindowContent,
                    "Lütfen dosya yolu ve change numarasını girin",
                    "Eksik Bilgi",
                    JOptionPane.WARNING_MESSAGE
                )
                return
            }

            try {
                val file = java.io.File(filePath)
                if (!file.exists()) {
                    JOptionPane.showMessageDialog(
                        toolWindowContent,
                        "Dosya bulunamadı: $filePath",
                        "Hata",
                        JOptionPane.ERROR_MESSAGE
                    )
                    return
                }

                val cleanChangeNo = changeNo.replace(Regex("[^0-9]"), "")

                if (cleanChangeNo.isEmpty()) {
                    JOptionPane.showMessageDialog(
                        toolWindowContent,
                        "Lütfen geçerli bir change numarası girin (örn: 144252)",
                        "Geçersiz Giriş",
                        JOptionPane.WARNING_MESSAGE
                    )
                    return
                }

                val content = file.readText()
                val updatedContent = content.replace(Regex("Dev-\\d+"), "Dev-$cleanChangeNo")

                val changeNumber = cleanChangeNo.toIntOrNull()
                val appTestReplacement = if (changeNumber != null && changeNumber % 2 == 0) {
                    "apptest2"
                } else {
                    "apptest"
                }

                val finalContent = updatedContent.replace(Regex("https://apptest2?\\."), "https://$appTestReplacement.")

                file.writeText(finalContent)

                JOptionPane.showMessageDialog(
                    toolWindowContent,
                    "Backend konfigürasyonu başarıyla güncellendi!\n\n" +
                            "Change No: Dev-$cleanChangeNo\n" +
                            "App Environment: $appTestReplacement",
                    "Başarılı",
                    JOptionPane.INFORMATION_MESSAGE
                )
            } catch (e: Exception) {
                JOptionPane.showMessageDialog(
                    toolWindowContent,
                    "Dosya güncellenirken hata oluştu: ${e.message}",
                    "Hata",
                    JOptionPane.ERROR_MESSAGE
                )
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