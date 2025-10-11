package org.jetbrains.plugins.template.designer.ui

import com.intellij.ui.JBColor
import java.awt.*
import javax.swing.*

object GradientPanel {
    fun create(): JPanel {
        return object : JPanel(BorderLayout()) {
            override fun paintComponent(g: Graphics) {
                val g2d = g as Graphics2D
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

                val gradient = GradientPaint(
                    0f, 0f, JBColor(Color(240, 242, 247), Color(30, 31, 34)),
                    width.toFloat(), height.toFloat(), JBColor(Color(230, 233, 240), Color(25, 26, 28))
                )
                g2d.paint = gradient
                g2d.fillRect(0, 0, width, height)

                super.paintComponent(g)
            }
        }.apply {
            isOpaque = true
        }
    }
}
