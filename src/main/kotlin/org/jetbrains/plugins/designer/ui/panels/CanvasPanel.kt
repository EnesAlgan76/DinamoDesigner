package org.jetbrains.plugins.template.designer.ui

import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import org.jetbrains.plugins.designer.models.ComponentInstance
import org.jetbrains.plugins.designer.models.ComponentManager
import org.jetbrains.plugins.designer.models.Screen
import org.jetbrains.plugins.template.designer.components.ComponentDefinition
import org.jetbrains.plugins.template.designer.components.ComponentRegistry
import java.awt.*
import java.awt.dnd.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

class CanvasPanel(
    private val componentManager: ComponentManager,
    private val onComponentSelected: (ComponentInstance) -> Unit
) : JPanel() {

    private var currentScreen: Screen? = null

    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        isOpaque = false
        border = JBUI.Borders.empty(20)

        setupDropTarget()
    }

    fun loadScreen(screen: Screen) {
        currentScreen = screen
        refreshComponents()
    }

    fun clearCanvas() {
        currentScreen?.components?.clear()
        removeAll()
        revalidate()
        repaint()
    }

    fun refreshComponents() {
        removeAll()

        currentScreen?.components?.forEach { component ->
            add(createComponentPreview(component))
        }

        revalidate()
        repaint()
    }

    fun addComponent(componentType: String, defaultProperties: Map<String, Any>) {
        val screen = currentScreen ?: return
        componentManager.addComponentToScreen(screen, componentType, defaultProperties)
        refreshComponents()

        SwingUtilities.invokeLater {
            val scrollPane = parent.parent as? JScrollPane
            scrollPane?.verticalScrollBar?.value = scrollPane?.verticalScrollBar?.maximum ?: 0
        }
    }

    private fun createComponentPreview(component: ComponentInstance): JPanel {
        return JPanel(BorderLayout()).apply {
            isOpaque = false
            border = JBUI.Borders.empty()
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)

            val definition = ComponentRegistry.getComponentByType(component.type)
            if (definition != null) {
                try {
                    val icon = definition.getDisplayIcon(null)
                    val scaledIcon = scaleIconForCanvas(icon)

                    val iconLabel = JLabel(scaledIcon).apply {
                        horizontalAlignment = SwingConstants.CENTER
                    }

                    add(iconLabel, BorderLayout.CENTER)
                    preferredSize = Dimension(scaledIcon.iconWidth, scaledIcon.iconHeight)
                    maximumSize = Dimension(500, scaledIcon.iconHeight)
                } catch (e: Exception) {
                    val label = JLabel(component.type).apply {
                        horizontalAlignment = SwingConstants.CENTER
                    }
                    add(label, BorderLayout.CENTER)
                }
            }

            // Silme butonu
            val deleteButton = JButton("×").apply {
                foreground = Color.WHITE
                font = Font(font.name, Font.BOLD, 16)
                isBorderPainted = false
                isFocusPainted = false
                isContentAreaFilled = true
                preferredSize = Dimension(20, 20)
                cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)

                addActionListener {
                    currentScreen?.let { screen ->
                        componentManager.removeComponent(screen, component.id)
                        refreshComponents()
                    }
                }
            }

            val deletePanel = JPanel(FlowLayout(FlowLayout.RIGHT, 5, 5)).apply {
                isOpaque = false
                add(deleteButton)
            }

            add(deletePanel, BorderLayout.EAST)

            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    onComponentSelected(component)
                }
            })
        }
    }

    private fun scaleIconForCanvas(icon: ImageIcon): ImageIcon {
        val maxWidth = 500
        var width = icon.iconWidth
        var height = icon.iconHeight

        if (width > maxWidth) {
            val scale = maxWidth.toDouble() / width
            width = maxWidth
            height = (height * scale).toInt()
        }

        val scaledImage = icon.image.getScaledInstance(width, height, Image.SCALE_SMOOTH)
        return ImageIcon(scaledImage)
    }

    private fun setupDropTarget() {
        DropTarget(this, object : DropTargetAdapter() {
            override fun drop(dtde: DropTargetDropEvent) {
                try {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY)
                    val transferable = dtde.transferable
                    val definition = transferable.getTransferData(ComponentDataFlavor) as ComponentDefinition

                    val defaultProps = definition.createDefaultProperties(
                        componentManager.getNextComponentId().substringAfter("_").toIntOrNull() ?: 0
                    )

                    addComponent(definition.type, defaultProps)
                    dtde.dropComplete(true)
                } catch (e: Exception) {
                    dtde.dropComplete(false)
                }
            }
        })
    }

    override fun paintComponent(g: Graphics) {
        val g2d = g as Graphics2D
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        g2d.color = JBColor(Color(255, 255, 255, 180), Color(35, 35, 35, 180))
        g2d.fillRoundRect(0, 0, width, height, 20, 20)

        //g2d.color = JBColor(Color(255, 255, 255, 100), Color(255, 255, 255, 30))
        //g2d.drawRoundRect(0, 0, width - 1, height - 1, 20, 20)

        super.paintComponent(g)
    }
}
