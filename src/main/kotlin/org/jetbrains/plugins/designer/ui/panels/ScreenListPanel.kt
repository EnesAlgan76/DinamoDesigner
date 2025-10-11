package org.jetbrains.plugins.template.designer.ui

import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import org.jetbrains.plugins.designer.models.Screen
import org.jetbrains.plugins.designer.models.ScreenManager
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

class ScreenListPanel(
    private val screenManager: ScreenManager,
    private val onScreenSelected: (Screen) -> Unit,
    private val onAddScreen: () -> Unit,
    private val onEditScreen: (Screen) -> Unit
) : JPanel(BorderLayout()) {

    private val screenItemsContainer = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        isOpaque = false
    }

    init {
        isOpaque = false
        border = JBUI.Borders.empty(0, 0, 20, 0)
        preferredSize = Dimension(280, 320)

        val titleLabel = StyledLabel("Screens", 16, Font.BOLD)
        add(titleLabel, BorderLayout.NORTH)

        val scrollPane = JScrollPane(screenItemsContainer).apply {
            preferredSize = Dimension(250, 220)
            border = JBUI.Borders.empty(15, 0, 15, 0)
            isOpaque = false
            viewport.isOpaque = false
        }
        add(scrollPane, BorderLayout.CENTER)

        val addButton = createAddButton()
        add(addButton, BorderLayout.SOUTH)

        refreshScreenList()
    }

    fun refreshScreenList() {
        screenItemsContainer.removeAll()

        screenManager.getAllScreens().forEach { screen ->
            val screenItem = createScreenItem(screen)
            screenItemsContainer.add(screenItem)
            screenItemsContainer.add(Box.createVerticalStrut(8))
        }

        screenItemsContainer.revalidate()
        screenItemsContainer.repaint()
    }

    fun selectScreen(screenId: String) {
        refreshScreenList()
    }

    private fun createScreenItem(screen: Screen): JPanel {
        return object : JPanel(BorderLayout()) {
            private var animationProgress = if (isSelected) 1f else 0f
            private val animationTimer = Timer(16) { repaint() }

            private val isSelected: Boolean
                get() = screenManager.getSelectedScreen()?.id == screen.id

            init {
                isOpaque = false
                border = JBUI.Borders.empty(10, 12)
                maximumSize = Dimension(Integer.MAX_VALUE, 60)
                cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)

                val nameLabel = JLabel(screen.name).apply {
                    font = Font("SF Pro Display", Font.BOLD, 13)
                }

                val entryBadge = JLabel("ENTRY").apply {
                    font = Font("SF Pro Display", Font.BOLD, 9)
                    foreground = Color.WHITE
                    background = Color(76, 175, 80)
                    isOpaque = true
                    border = JBUI.Borders.empty(2, 6)
                    isVisible = screen.isEntryScreen
                }

                val typeLabel = JLabel("${screen.type.name} Screen").apply {
                    font = Font("SF Pro Display", Font.PLAIN, 12)
                }

                addComponentListener(object : java.awt.event.ComponentAdapter() {
                    override fun componentShown(e: java.awt.event.ComponentEvent?) {
                        nameLabel.foreground = if (isSelected) Color.WHITE else JBColor(Color(30, 41, 59), Color(241, 245, 249))
                        typeLabel.foreground = if (isSelected) Color(230, 230, 230) else JBColor(Color(100, 116, 139), Color(148, 163, 184))
                    }
                })


                val textPanel = JPanel().apply {
                    layout = BoxLayout(this, BoxLayout.Y_AXIS)
                    isOpaque = false

                    val nameBadgePanel = JPanel().apply {
                        layout = BoxLayout(this, BoxLayout.X_AXIS)
                        isOpaque = false
                        add(nameLabel)
                        add(Box.createHorizontalStrut(8))
                        add(entryBadge)
                        add(Box.createHorizontalGlue())
                    }

                    add(nameBadgePanel)
                    add(Box.createVerticalStrut(4))
                    add(typeLabel)
                }

                add(textPanel, BorderLayout.CENTER)

                val settingsButton = JButton("⚙").apply {
                    font = Font("SF Pro Display", Font.PLAIN, 28)
                    preferredSize = Dimension(48, 48)
                    isOpaque = false
                    isBorderPainted = false
                    isContentAreaFilled = false
                    isFocusPainted = false
                    cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                    foreground = if (isSelected) Color(230, 230, 230) else JBColor(Color(100, 116, 139), Color(148, 163, 184))

                    addMouseListener(object : MouseAdapter() {
                        override fun mouseEntered(e: MouseEvent) {
                            foreground = if (isSelected) Color.WHITE else JBColor(Color(30, 41, 59), Color(241, 245, 249))
                        }

                        override fun mouseExited(e: MouseEvent) {
                            foreground = if (isSelected) Color(230, 230, 230) else JBColor(Color(100, 116, 139), Color(148, 163, 184))
                        }
                    })

                    addActionListener {
                        onEditScreen(screen)
                    }
                }

                add(settingsButton, BorderLayout.EAST)

                addMouseListener(object : MouseAdapter() {
                    override fun mouseClicked(e: MouseEvent) {
                        onScreenSelected(screen)
                    }

                    override fun mouseEntered(e: MouseEvent) {
                        if (!isSelected) animateHover(true)
                    }

                    override fun mouseExited(e: MouseEvent) {
                        if (!isSelected) animateHover(false)
                    }
                })
            }

            override fun paintComponent(g: Graphics) {
                val g2d = g as Graphics2D
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

                if (isSelected) {
                    g2d.color = Color(76, 175, 80, 220)
                    g2d.fillRoundRect(0, 0, width, height, 12, 12)
                } else {
                    val bgAlpha = (230 + animationProgress * 25).toInt()
                    g2d.color = JBColor(Color(255, 255, 255, bgAlpha), Color(50, 50, 50, bgAlpha))
                    g2d.fillRoundRect(0, 0, width, height, 12, 12)
                }

                val borderAlpha = if (isSelected) 150 else (60 + animationProgress * 80).toInt()
                g2d.color = Color(255, 255, 255, borderAlpha)
                g2d.drawRoundRect(0, 0, width - 1, height - 1, 12, 12)

                super.paintComponent(g)
            }

            private fun animateHover(hover: Boolean) {
                animationTimer.stop()
                val targetProgress = if (hover) 1f else 0f

                animationTimer.addActionListener {
                    animationProgress = if (hover) {
                        minOf(animationProgress + 0.12f, targetProgress)
                    } else {
                        maxOf(animationProgress - 0.12f, targetProgress)
                    }
                    if (animationProgress == targetProgress) animationTimer.stop()
                    repaint()
                }
                animationTimer.start()
            }
        }
    }

    private fun createAddButton(): JButton {
        return ModernButton("+ Add Screen", ModernButtonStyle.SUCCESS, onAddScreen).apply {
            maximumSize = Dimension(Integer.MAX_VALUE, 40)
        }
    }
}
