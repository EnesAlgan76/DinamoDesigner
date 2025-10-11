package org.jetbrains.plugins.template.designer.ui

import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

class AnimatedCard(
    private val title: String,
    private val onRemove: () -> Unit
) : JPanel() {

    private var isDragging = false
    private var isHovered = false
    private var animationProgress = 0f
    private var dragOffsetX = 0
    private var dragOffsetY = 0

    private val animationTimer = Timer(16) { repaint() }

    init {
        layout = BorderLayout()
        isOpaque = false
        cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)

        createContent()
        setupDragBehavior()
        setupHoverAnimation()
    }

    private fun createContent() {
        // Title
        val titleLabel = JLabel(title).apply {
            font = Font("SF Pro Display", Font.BOLD, 16)
            foreground = JBColor(Color(30, 41, 59), Color(241, 245, 249))
            horizontalAlignment = SwingConstants.CENTER
        }

        // Content
        val contentLabel = JLabel("<html><center><div style='font-size: 24px;'>🎨</div><br/><span style='color: #64748b;'>Card</span></center></html>").apply {
            horizontalAlignment = SwingConstants.CENTER
        }

        // Delete button
        val deleteButton = object : JButton("×") {
            override fun paintComponent(g: Graphics) {
                val g2d = g as Graphics2D
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

                g2d.color = Color(244, 67, 54, if (model.isPressed) 255 else 200)
                g2d.fillOval(0, 0, width, height)

                g2d.color = Color(255, 255, 255, 150)
                g2d.drawOval(0, 0, width - 1, height - 1)

                super.paintComponent(g)
            }
        }.apply {
            preferredSize = Dimension(24, 24)
            font = Font("SF Pro Display", Font.BOLD, 14)
            foreground = Color.WHITE
            isFocusPainted = false
            isBorderPainted = false
            isContentAreaFilled = false
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)

            addActionListener { onRemove() }
        }

        val topPanel = JPanel(BorderLayout()).apply {
            isOpaque = false
            border = JBUI.Borders.empty(5)
            add(titleLabel, BorderLayout.CENTER)
            add(deleteButton, BorderLayout.EAST)
        }

        add(topPanel, BorderLayout.NORTH)
        add(contentLabel, BorderLayout.CENTER)
    }

    fun setupDragBehavior() {
        val mouseAdapter = object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                isDragging = true
                dragOffsetX = e.x
                dragOffsetY = e.y
                cursor = Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR)
                animateHover(true)
            }

            override fun mouseReleased(e: MouseEvent) {
                isDragging = false
                cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                animateHover(false)
            }

            override fun mouseDragged(e: MouseEvent) {
                if (isDragging) {
                    val newX = location.x + e.x - dragOffsetX
                    val newY = location.y + e.y - dragOffsetY

                    val maxX = parent.width - width - 20
                    val maxY = parent.height - height - 20

                    val constrainedX = maxOf(20, minOf(newX, maxX))
                    val constrainedY = maxOf(20, minOf(newY, maxY))

                    setLocation(constrainedX, constrainedY)
                }
            }
        }

        addMouseListener(mouseAdapter)
        addMouseMotionListener(mouseAdapter)
    }

    private fun setupHoverAnimation() {
        addMouseListener(object : MouseAdapter() {
            override fun mouseEntered(e: MouseEvent) {
                if (!isDragging) {
                    isHovered = true
                    animateHover(true)
                }
            }

            override fun mouseExited(e: MouseEvent) {
                if (!isDragging) {
                    isHovered = false
                    animateHover(false)
                }
            }
        })
    }

    fun animateIn() {
        // Simple fade in
        var alpha = 0f
        val timer = Timer(16) {
            alpha += 0.05f
            if (alpha >= 1f) {
                (it.source as Timer).stop()
            }
            repaint()
        }
        timer.start()
    }

    fun animateOut(onComplete: () -> Unit) {
        val fadeTimer = Timer(16, null)
        var alpha = 1f

        fadeTimer.addActionListener {
            alpha -= 0.1f
            if (alpha <= 0f) {
                fadeTimer.stop()
                onComplete()
            } else {
                repaint()
            }
        }
        fadeTimer.start()
    }

    private fun animateHover(hover: Boolean) {
        animationTimer.stop()
        val targetProgress = if (hover) 1f else 0f

        animationTimer.addActionListener {
            val step = 0.15f
            animationProgress = if (hover) {
                minOf(animationProgress + step, targetProgress)
            } else {
                maxOf(animationProgress - step, targetProgress)
            }

            if (animationProgress == targetProgress) {
                animationTimer.stop()
            }
            repaint()
        }
        animationTimer.start()
    }

    override fun paintComponent(g: Graphics) {
        val g2d = g as Graphics2D
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        // Shadow
        val shadowOffset = (4 + animationProgress * 8).toInt()
        val shadowAlpha = (20 + animationProgress * 30).toInt()

        g2d.color = Color(0, 0, 0, shadowAlpha)
        g2d.fillRoundRect(shadowOffset, shadowOffset, width - shadowOffset, height - shadowOffset, 20, 20)

        // Card background
        val bgAlpha = (240 + animationProgress * 15).toInt()
        g2d.color = JBColor(Color(255, 255, 255, bgAlpha), Color(30, 31, 34, bgAlpha))
        g2d.fillRoundRect(0, 0, width, height, 20, 20)

        // Border
        val borderAlpha = (100 + animationProgress * 100).toInt()
        g2d.color = JBColor(Color(200, 200, 255, borderAlpha), Color(100, 150, 255, borderAlpha))
        g2d.drawRoundRect(0, 0, width - 1, height - 1, 20, 20)

        // Inner highlight
        g2d.color = Color(255, 255, 255, (30 + animationProgress * 50).toInt())
        g2d.drawRoundRect(1, 1, width - 3, height - 3, 18, 18)

        super.paintComponent(g)
    }
}
