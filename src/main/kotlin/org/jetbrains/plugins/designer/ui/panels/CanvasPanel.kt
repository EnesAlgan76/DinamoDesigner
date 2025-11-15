package org.jetbrains.plugins.template.designer.ui

import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import org.jetbrains.plugins.designer.models.ComponentInstance
import org.jetbrains.plugins.designer.models.ComponentManager
import org.jetbrains.plugins.designer.models.Screen
import org.jetbrains.plugins.template.designer.components.ComponentDefinition
import org.jetbrains.plugins.template.designer.components.ComponentRegistry
import java.awt.*
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.dnd.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

// DataFlavor for ComponentInstance drag and drop
private val ComponentInstanceFlavor = DataFlavor(ComponentInstance::class.java, "ComponentInstance")

class CanvasPanel(
    private val componentManager: ComponentManager,
    private val onComponentSelected: (ComponentInstance) -> Unit,
    private val onComponentAdded: ((Screen) -> Unit)? = null,
    private val onComponentDeleted: ((Screen) -> Unit)? = null
) : JPanel() {

    private var currentScreen: Screen? = null
    private var selectedComponent: ComponentInstance? = null
    private val mainContentPanel = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        isOpaque = false
    }
    private val footerPanel = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        isOpaque = false
        border = JBUI.Borders.empty(20, 0, 0, 0)
    }

    init {
        layout = BorderLayout()
        isOpaque = false
        border = JBUI.Borders.empty(20)

        val scrollPane = JScrollPane(mainContentPanel).apply {
            isOpaque = false
            viewport.isOpaque = false
            border = JBUI.Borders.empty()
        }

        add(scrollPane, BorderLayout.CENTER)
        add(createFooterContainer(), BorderLayout.SOUTH)

        setupDropTarget()
    }

    private fun createFooterContainer(): JPanel {
        return JPanel(BorderLayout()).apply {
            isOpaque = false
            border = JBUI.Borders.empty(10, 0, 0, 0)

            val separator = JSeparator().apply {
                foreground = JBColor.border()
            }
            add(separator, BorderLayout.NORTH)

            val footerTitlePanel = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
                isOpaque = false
                add(JLabel("Footer Buttons").apply {
                    font = Font("SF Pro Display", Font.BOLD, 12)
                    foreground = JBColor(Color(100, 116, 139), Color(148, 163, 184))
                })
            }
            add(footerTitlePanel, BorderLayout.NORTH)

            add(footerPanel, BorderLayout.CENTER)
        }
    }

    fun loadScreen(screen: Screen) {
        currentScreen = screen
        refreshComponents()
    }

    fun clearCanvas() {
        currentScreen?.components?.clear()
        currentScreen?.footerComponents?.clear()
        mainContentPanel.removeAll()
        footerPanel.removeAll()
        revalidate()
        repaint()
    }

    fun refreshComponents() {
        mainContentPanel.removeAll()
        footerPanel.removeAll()

        currentScreen?.components?.forEach { component ->
            mainContentPanel.add(createComponentPreview(component, isFooter = false))
        }

        currentScreen?.footerComponents?.forEach { component ->
            footerPanel.add(createComponentPreview(component, isFooter = true))
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

        onComponentAdded?.invoke(screen)
    }

    private fun createComponentPreview(component: ComponentInstance, isFooter: Boolean): JPanel {
        return JPanel(BorderLayout()).apply {
            isOpaque = false
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)

            // Update border based on selection state
            val isSelected = selectedComponent?.id == component.id
            border = if (isSelected) {
                JBUI.Borders.customLine(JBColor(Color(59, 130, 246), Color(96, 165, 250)), 2)
            } else {
                JBUI.Borders.empty()
            }

            // Background highlight for selected component
            if (isSelected) {
                isOpaque = true
                background = JBColor(Color(59, 130, 246, 20), Color(96, 165, 250, 20))
            }

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
                        if (isFooter) {
                            screen.footerComponents.removeIf { it.id == component.id }
                        } else {
                            componentManager.removeComponent(screen, component.id)
                        }
                        refreshComponents()
                        onComponentDeleted?.invoke(screen)
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
                    selectedComponent = component
                    onComponentSelected(component)
                    refreshComponents()
                }
            })

            // Drag and drop for reordering
            setupComponentDragAndDrop(this, component, isFooter)
        }
    }

    private fun setupComponentDragAndDrop(panel: JPanel, component: ComponentInstance, isFooter: Boolean) {
        val dragSource = DragSource.getDefaultDragSource()

        dragSource.createDefaultDragGestureRecognizer(panel, DnDConstants.ACTION_MOVE,
            object : DragGestureListener {
                override fun dragGestureRecognized(dge: DragGestureEvent) {
                    val transferable = object : Transferable {
                        override fun getTransferDataFlavors() = arrayOf(ComponentInstanceFlavor)
                        override fun isDataFlavorSupported(flavor: DataFlavor?) = flavor == ComponentInstanceFlavor
                        override fun getTransferData(flavor: DataFlavor?) = component
                    }
                    dge.startDrag(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR), transferable)
                }
            })

        DropTarget(panel, object : DropTargetAdapter() {
            override fun dragOver(dtde: DropTargetDragEvent) {
                dtde.acceptDrag(DnDConstants.ACTION_MOVE)
            }

            override fun drop(dtde: DropTargetDropEvent) {
                try {
                    dtde.acceptDrop(DnDConstants.ACTION_MOVE)
                    val transferable = dtde.transferable

                    if (transferable.isDataFlavorSupported(ComponentInstanceFlavor)) {
                        val draggedComponent = transferable.getTransferData(ComponentInstanceFlavor) as ComponentInstance
                        val screen = currentScreen ?: return

                        val list = if (isFooter) screen.footerComponents else screen.components
                        val draggedIndex = list.indexOfFirst { it.id == draggedComponent.id }
                        val targetIndex = list.indexOfFirst { it.id == component.id }

                        if (draggedIndex != -1 && targetIndex != -1 && draggedIndex != targetIndex) {
                            list.removeAt(draggedIndex)
                            list.add(targetIndex, draggedComponent)
                            refreshComponents()
                            onComponentAdded?.invoke(screen)
                        }

                        dtde.dropComplete(true)
                    } else {
                        dtde.dropComplete(false)
                    }
                } catch (e: Exception) {
                    dtde.dropComplete(false)
                }
            }
        })
    }

    private fun scaleIconForCanvas(icon: ImageIcon): ImageIcon {
        val maxWidth = 350
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
        // Main content drop target
        DropTarget(mainContentPanel, object : DropTargetAdapter() {
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

        // Footer drop target
        DropTarget(footerPanel, object : DropTargetAdapter() {
            override fun drop(dtde: DropTargetDropEvent) {
                try {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY)
                    val transferable = dtde.transferable
                    val definition = transferable.getTransferData(ComponentDataFlavor) as ComponentDefinition

                    if (definition.type != "BUTTON") {
                        dtde.dropComplete(false)
                        return
                    }

                    addFooterComponentFromDefinition(definition)
                    dtde.dropComplete(true)
                } catch (e: Exception) {
                    dtde.dropComplete(false)
                }
            }
        })
    }

    fun addFooterComponentFromDefinition(definition: ComponentDefinition) {
        val screen = currentScreen ?: return

        // Use ComponentManager to create the component with proper counter management
        val component = componentManager.createComponent(definition)

        // Add to footer components instead of main components
        screen.footerComponents.add(component)

        refreshComponents()
        onComponentAdded?.invoke(screen)
    }

    fun addFooterComponent(componentType: String, defaultProperties: Map<String, Any>) {
        val screen = currentScreen ?: return
        val newComponent = ComponentInstance(
            id = componentManager.getNextComponentId(),
            type = componentType,
            properties = defaultProperties.toMutableMap()
        )
        screen.footerComponents.add(newComponent)
        refreshComponents()
        onComponentAdded?.invoke(screen)
    }

    override fun paintComponent(g: Graphics) {
        val g2d = g as Graphics2D
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        g2d.color = JBColor(Color(255, 255, 255, 180), Color(35, 35, 35, 180))
        g2d.fillRoundRect(0, 0, width, height, 20, 20)

        super.paintComponent(g)
    }
}
