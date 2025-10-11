package org.jetbrains.plugins.template.designer.ui

import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import org.jetbrains.plugins.template.designer.components.ComponentDefinition
import org.jetbrains.plugins.template.designer.components.ComponentRegistry
import java.awt.*
import java.awt.dnd.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

class ComponentLibraryPanel : JPanel() {

    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        isOpaque = false

        val titleLabel = StyledLabel("Component Library", 16, Font.BOLD).apply {
            alignmentX = Component.CENTER_ALIGNMENT
        }
        add(titleLabel)
        add(Box.createVerticalStrut(15))

        initialize()
    }

    fun initialize() {
        ComponentRegistry.getAllComponents().forEach { definition ->
            add(createComponentItem(definition))
            add(Box.createVerticalStrut(10))
        }
    }

    private fun createComponentItem(definition: ComponentDefinition): JPanel {
        return object : JPanel() {
            private var animationProgress = 0f
            private val animationTimer = Timer(16) { repaint() }

            init {
                layout = BorderLayout()
                isOpaque = false
                border = JBUI.Borders.empty(5)
                cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)

                try {
                    val icon = definition.getDisplayIcon(null)
                    val scaledIcon = scaleIcon(icon, 240, 80)

                    val iconLabel = JLabel(scaledIcon).apply {
                        horizontalAlignment = SwingConstants.CENTER
                    }

                    preferredSize = Dimension(240, scaledIcon.iconHeight + 10)
                    maximumSize = Dimension(280, scaledIcon.iconHeight + 10)

                    add(iconLabel, BorderLayout.CENTER)
                } catch (e: Exception) {
                    val fallbackLabel = JLabel(definition.displayName).apply {
                        horizontalAlignment = SwingConstants.CENTER
                        font = Font("SF Pro Display", Font.PLAIN, 12)
                    }
                    preferredSize = Dimension(240, 60)
                    maximumSize = Dimension(280, 60)
                    add(fallbackLabel, BorderLayout.CENTER)
                }

                addMouseListener(object : MouseAdapter() {
                    override fun mouseEntered(e: MouseEvent) = animateHover(true)
                    override fun mouseExited(e: MouseEvent) = animateHover(false)
                })

                setupDragAndDrop(this, definition)
            }

            override fun paintComponent(g: Graphics) {
                val g2d = g as Graphics2D
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

                val shadowAlpha = (20 + animationProgress * 30).toInt()
                g2d.color = Color(0, 0, 0, shadowAlpha)
                g2d.fillRoundRect(2, 2, width - 2, height - 2, 12, 12)

                val bgAlpha = (240 + animationProgress * 15).toInt()
                g2d.color = JBColor(Color(255, 255, 255, bgAlpha), Color(45, 45, 45, bgAlpha))
                g2d.fillRoundRect(0, 0, width, height, 12, 12)

                val borderAlpha = (80 + animationProgress * 80).toInt()
                g2d.color = JBColor(Color(200, 200, 255, borderAlpha), Color(100, 150, 255, borderAlpha))
                g2d.drawRoundRect(0, 0, width - 1, height - 1, 12, 12)

                super.paintComponent(g)
            }

            private fun animateHover(hover: Boolean) {
                animationTimer.stop()
                val targetProgress = if (hover) 1f else 0f

                animationTimer.addActionListener {
                    animationProgress = if (hover) {
                        minOf(animationProgress + 0.15f, targetProgress)
                    } else {
                        maxOf(animationProgress - 0.15f, targetProgress)
                    }
                    if (animationProgress == targetProgress) animationTimer.stop()
                    repaint()
                }
                animationTimer.start()
            }
        }
    }

    private fun scaleIcon(icon: ImageIcon, maxWidth: Int, maxHeight: Int): ImageIcon {
        var width = icon.iconWidth
        var height = icon.iconHeight

        if (width > maxWidth) {
            val scale = maxWidth.toDouble() / width
            width = maxWidth
            height = (height * scale).toInt()
        }

        if (height > maxHeight) {
            val scale = maxHeight.toDouble() / height
            height = maxHeight
            width = (width * scale).toInt()
        }

        val scaledImage = icon.image.getScaledInstance(width, height, Image.SCALE_SMOOTH)
        return ImageIcon(scaledImage)
    }

    private fun setupDragAndDrop(panel: JPanel, definition: ComponentDefinition) {
        val dragSource = DragSource.getDefaultDragSource()
        dragSource.createDefaultDragGestureRecognizer(
            panel,
            DnDConstants.ACTION_COPY,
            object : DragGestureListener {
                override fun dragGestureRecognized(dge: DragGestureEvent) {
                    dge.startDrag(
                        Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR),
                        ComponentTransferable(definition)
                    )
                }
            }
        )
    }
}
