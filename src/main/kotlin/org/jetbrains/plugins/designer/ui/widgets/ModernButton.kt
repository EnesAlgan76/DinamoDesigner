package org.jetbrains.plugins.template.designer.ui

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

class ModernButton(
    text: String,
    private val style: ModernButtonStyle,
    private val onClick: () -> Unit
) : JButton(text) {

    private var animationProgress = 0f
    private val animationTimer = Timer(16) { repaint() }

    init {
        font = Font("SF Pro Display", Font.BOLD, 13)
        foreground = Color.WHITE
        isFocusPainted = false
        isBorderPainted = false
        isContentAreaFilled = false
        cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        preferredSize = Dimension(200, 35)

        addActionListener { onClick() }
        setupAnimation()
    }

    private fun setupAnimation() {
        addMouseListener(object : MouseAdapter() {
            override fun mouseEntered(e: MouseEvent) = animateHover(true)
            override fun mouseExited(e: MouseEvent) = animateHover(false)
        })
    }

    private fun animateHover(hover: Boolean) {
        animationTimer.stop()
        val targetProgress = if (hover) 1f else 0f

        animationTimer.addActionListener {
            animationProgress = if (hover) {
                minOf(animationProgress + 0.1f, targetProgress)
            } else {
                maxOf(animationProgress - 0.1f, targetProgress)
            }
            if (animationProgress == targetProgress) animationTimer.stop()
            repaint()
        }
        animationTimer.start()
    }

    override fun paintComponent(g: Graphics) {
        val g2d = g as Graphics2D
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        val baseColor = style.color
        val alpha = (200 + animationProgress * 55).toInt()
        val shadowOffset = (2 + animationProgress * 3).toInt()

        // Shadow
        g2d.color = Color(0, 0, 0, (30 + animationProgress * 20).toInt())
        g2d.fillRoundRect(shadowOffset, shadowOffset, width - shadowOffset, height - shadowOffset, 12, 12)

        // Button background
        g2d.color = Color(baseColor.red, baseColor.green, baseColor.blue, alpha)
        g2d.fillRoundRect(0, 0, width, height, 12, 12)

        // Border highlight
        g2d.color = Color(255, 255, 255, (50 + animationProgress * 50).toInt())
        g2d.drawRoundRect(0, 0, width - 1, height - 1, 12, 12)

        super.paintComponent(g)
    }
}
