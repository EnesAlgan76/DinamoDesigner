package org.jetbrains.plugins.template.designer.ui

import com.intellij.ui.JBColor
import java.awt.*
import javax.swing.*

class StyledLabel(
    text: String,
    fontSize: Int,
    fontStyle: Int
) : JLabel(text) {
    init {
        font = Font("SF Pro Display", fontStyle, fontSize)
        foreground = JBColor(Color(30, 41, 59), Color(241, 245, 249))
    }
}
