package org.jetbrains.plugins.template.designer.ui

import com.intellij.ui.JBColor
import java.awt.*
import javax.swing.*

class GlassmorphicPanel : JPanel(BorderLayout()) {

    init {
        isOpaque = false
    }

    override fun paintComponent(g: Graphics) {
        val g2d = g as Graphics2D
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        g2d.color = JBColor(Color(255, 255, 255, 200), Color(40, 40, 40, 200))
        g2d.fillRoundRect(0, 0, width, height, 20, 20)

        g2d.color = JBColor(Color(255, 255, 255, 100), Color(255, 255, 255, 30))
        g2d.drawRoundRect(0, 0, width - 1, height - 1, 20, 20)

        super.paintComponent(g)
    }
}
