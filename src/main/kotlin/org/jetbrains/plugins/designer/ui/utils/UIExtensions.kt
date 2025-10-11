package org.jetbrains.plugins.template.designer.ui

import java.awt.*
import javax.swing.*

fun JPanel.applyGlassmorphism() {
    isOpaque = false
}

fun Component.fadeIn(duration: Int = 300) {
    var alpha = 0f
    val step = 50f / duration

    val timer = Timer(16) {
        alpha += step
        if (alpha >= 1f) {
            (it.source as Timer).stop()
        }
        repaint()
    }
    timer.start()
}

fun Component.fadeOut(duration: Int = 300, onComplete: () -> Unit) {
    var alpha = 1f
    val step = 50f / duration

    val timer = Timer(16) {
        alpha -= step
        if (alpha <= 0f) {
            (it.source as Timer).stop()
            onComplete()
        }
        repaint()
    }
    timer.start()
}
