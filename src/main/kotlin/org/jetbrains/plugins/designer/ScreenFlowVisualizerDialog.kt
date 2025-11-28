package org.jetbrains.plugins.designer

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.JBColor
import org.jetbrains.plugins.designer.models.Screen
import org.jetbrains.plugins.designer.models.ScreenType
import org.jetbrains.plugins.template.designer.components.ComponentRegistry
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import java.awt.geom.AffineTransform
import java.awt.geom.CubicCurve2D
import java.awt.geom.Point2D
import java.awt.image.BufferedImage
import javax.swing.*
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class ScreenFlowVisualizerDialog(
    project: Project,
    private val screens: List<Screen>
) : DialogWrapper(project) {

    private lateinit var canvas: FlowCanvas
    private val cards = mutableListOf<ScreenCard>()
    private val connections = mutableListOf<Connection>()

    init {
        title = "Screen Flow Visualizer"
        setSize(1200, 700)
        init()
        initializeCards()
    }

    override fun createCenterPanel(): JComponent {
        canvas = FlowCanvas()
        return canvas
    }

    private fun initializeCards() {
        screens.forEachIndexed { index, screen ->
            val x = 100 + (index % 4) * 250
            val y = 100 + (index / 4) * 400
            val card = ScreenCard(screen, x, y)
            cards.add(card)
        }

        screens.forEach { screen ->
            val allComponents = screen.components + screen.footerComponents

            allComponents.forEach { component ->
                val targetScreenId = component.properties["targetScreen"] as? String
                if (!targetScreenId.isNullOrEmpty()) {
                    val sourceCard = cards.find { it.screen.id == screen.id }
                    val targetCard = cards.find { it.screen.id == targetScreenId }

                    if (sourceCard != null && targetCard != null) {
                        val buttonLabel = component.properties["text"] as? String
                        connections.add(Connection(sourceCard, targetCard, buttonLabel))
                    }
                }
            }
        }

        canvas.repaint()
    }

    inner class FlowCanvas : JPanel() {
        private var draggedCard: ScreenCard? = null
        private var dragOffsetX = 0
        private var dragOffsetY = 0
        private var hoveredCard: ScreenCard? = null

        private var zoomLevel = 1.0
        private var panX = 0.0
        private var panY = 0.0
        private var isPanning = false
        private var lastPanX = 0
        private var lastPanY = 0

        init {
            background = JBColor(Color(240, 242, 247), Color(30, 31, 34))
            preferredSize = Dimension(1200, 700)

            setupMouseListeners()
        }

        private fun setupMouseListeners() {
            val mouseAdapter = object : MouseAdapter() {
                override fun mousePressed(e: MouseEvent) {
                    val transformedPoint = screenToWorld(e.x, e.y)
                    val card = findCardAt(transformedPoint.x.toInt(), transformedPoint.y.toInt())

                    if (e.isMetaDown || e.isControlDown || SwingUtilities.isMiddleMouseButton(e)) {
                        isPanning = true
                        lastPanX = e.x
                        lastPanY = e.y
                        cursor = Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR)
                    } else if (card != null) {
                        draggedCard = card
                        dragOffsetX = transformedPoint.x.toInt() - card.x
                        dragOffsetY = transformedPoint.y.toInt() - card.y
                        cursor = Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR)
                    } else {
                        isPanning = true
                        lastPanX = e.x
                        lastPanY = e.y
                        cursor = Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR)
                    }
                }

                override fun mouseReleased(e: MouseEvent) {
                    draggedCard = null
                    isPanning = false
                    cursor = Cursor.getDefaultCursor()
                }

                override fun mouseDragged(e: MouseEvent) {
                    if (isPanning) {
                        val dx = e.x - lastPanX
                        val dy = e.y - lastPanY
                        panX += dx
                        panY += dy
                        lastPanX = e.x
                        lastPanY = e.y
                        repaint()
                    } else {
                        draggedCard?.let { card ->
                            val transformedPoint = screenToWorld(e.x, e.y)
                            card.x = transformedPoint.x.toInt() - dragOffsetX
                            card.y = transformedPoint.y.toInt() - dragOffsetY
                            repaint()
                        }
                    }
                }

                override fun mouseMoved(e: MouseEvent) {
                    val transformedPoint = screenToWorld(e.x, e.y)
                    val newHovered = findCardAt(transformedPoint.x.toInt(), transformedPoint.y.toInt())
                    if (newHovered != hoveredCard) {
                        hoveredCard = newHovered
                        cursor = if (newHovered != null) {
                            Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                        } else {
                            Cursor.getDefaultCursor()
                        }
                        repaint()
                    }
                }

                override fun mouseWheelMoved(e: MouseWheelEvent) {
                    val oldZoom = zoomLevel
                    val zoomFactor = if (e.isMetaDown || e.isControlDown) 0.05 else 0.1

                    if (e.wheelRotation < 0) {
                        zoomLevel = (zoomLevel + zoomFactor).coerceIn(0.3, 3.0)
                    } else {
                        zoomLevel = (zoomLevel - zoomFactor).coerceIn(0.3, 3.0)
                    }

                    val mouseX = e.x.toDouble()
                    val mouseY = e.y.toDouble()
                    val zoomRatio = zoomLevel / oldZoom

                    panX = mouseX - (mouseX - panX) * zoomRatio
                    panY = mouseY - (mouseY - panY) * zoomRatio

                    repaint()
                }
            }

            addMouseListener(mouseAdapter)
            addMouseMotionListener(mouseAdapter)
            addMouseWheelListener(mouseAdapter)
        }

        private fun screenToWorld(screenX: Int, screenY: Int): Point2D {
            val worldX = (screenX - panX) / zoomLevel
            val worldY = (screenY - panY) / zoomLevel
            return Point2D.Double(worldX, worldY)
        }

        private fun findCardAt(x: Int, y: Int): ScreenCard? {
            return cards.findLast { card ->
                x >= card.x && x <= card.x + card.width &&
                        y >= card.y && y <= card.y + card.height
            }
        }

        override fun paintComponent(g: Graphics) {
            super.paintComponent(g)
            val g2d = g as Graphics2D
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

            val originalTransform = g2d.transform

            val transform = AffineTransform()
            transform.translate(panX, panY)
            transform.scale(zoomLevel, zoomLevel)
            g2d.transform(transform)

            connections.forEach { connection ->
                drawConnection(g2d, connection)
            }

            cards.forEach { card ->
                val isHovered = card == hoveredCard
                val isDragged = card == draggedCard
                drawCard(g2d, card, isHovered, isDragged)
            }

            g2d.transform = originalTransform

            g2d.color = JBColor(Color(100, 100, 100, 180), Color(200, 200, 200, 180))
            g2d.font = Font("SF Pro Display", Font.PLAIN, 11)
            g2d.drawString("Zoom: ${(zoomLevel * 100).toInt()}%", 10, height - 10)
        }

        private fun drawConnection(g2d: Graphics2D, connection: Connection) {
            val source = connection.source
            val target = connection.target

            val startX = source.x + source.width / 2.0
            val startY = source.y + source.height / 2.0
            val endX = target.x + target.width / 2.0
            val endY = target.y + target.height / 2.0

            val dx = endX - startX
            val dy = endY - startY
            val distance = Math.sqrt(dx * dx + dy * dy)

            val controlOffset = distance * 0.3
            val ctrl1X = startX + dx * 0.25
            val ctrl1Y = startY - controlOffset
            val ctrl2X = startX + dx * 0.75
            val ctrl2Y = endY - controlOffset

            val isContinueConnection = connection.label == "Continue"
            val lineColor = if (isContinueConnection) {
                JBColor(Color(46, 204, 113, 180), Color(46, 204, 113, 180))
            } else {
                JBColor(Color(52, 152, 219, 180), Color(52, 152, 219, 180))
            }

            g2d.color = lineColor
            g2d.stroke = BasicStroke(
                3f,
                BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND,
                1.0f,
                floatArrayOf(10f, 5f),
                0f
            )

            val curve = CubicCurve2D.Double(startX, startY, ctrl1X, ctrl1Y, ctrl2X, ctrl2Y, endX, endY)
            g2d.draw(curve)

            val angle = atan2(endY - ctrl2Y, endX - ctrl2X)
            val arrowSize = 16

            val arrow = Polygon()
            arrow.addPoint(endX.toInt(), endY.toInt())
            arrow.addPoint(
                (endX - arrowSize * cos(angle - Math.PI / 6)).toInt(),
                (endY - arrowSize * sin(angle - Math.PI / 6)).toInt()
            )
            arrow.addPoint(
                (endX - arrowSize * cos(angle + Math.PI / 6)).toInt(),
                (endY - arrowSize * sin(angle + Math.PI / 6)).toInt()
            )

            g2d.stroke = BasicStroke(1f)
            g2d.fill(arrow)

            val arrowBorderColor = if (isContinueConnection) {
                JBColor(Color(39, 174, 96), Color(39, 174, 96))
            } else {
                JBColor(Color(41, 128, 185), Color(41, 128, 185))
            }
            g2d.color = arrowBorderColor
            g2d.draw(arrow)

            drawDirectionArrows(g2d, curve, isContinueConnection)
        }

        private fun drawDirectionArrows(g2d: Graphics2D, curve: CubicCurve2D.Double, isContinue: Boolean = false) {
            val numArrows = 2
            val arrowColor = if (isContinue) {
                JBColor(Color(46, 204, 113, 200), Color(46, 204, 113, 200))
            } else {
                JBColor(Color(52, 152, 219, 200), Color(52, 152, 219, 200))
            }
            g2d.color = arrowColor

            for (i in 1..numArrows) {
                val t = i / (numArrows + 1.0)

                val x = (1-t)*(1-t)*(1-t) * curve.x1 +
                        3*(1-t)*(1-t)*t * curve.ctrlx1 +
                        3*(1-t)*t*t * curve.ctrlx2 +
                        t*t*t * curve.x2

                val y = (1-t)*(1-t)*(1-t) * curve.y1 +
                        3*(1-t)*(1-t)*t * curve.ctrly1 +
                        3*(1-t)*t*t * curve.ctrly2 +
                        t*t*t * curve.y2

                val dx = 3*(1-t)*(1-t) * (curve.ctrlx1 - curve.x1) +
                        6*(1-t)*t * (curve.ctrlx2 - curve.ctrlx1) +
                        3*t*t * (curve.x2 - curve.ctrlx2)

                val dy = 3*(1-t)*(1-t) * (curve.ctrly1 - curve.y1) +
                        6*(1-t)*t * (curve.ctrly2 - curve.ctrly1) +
                        3*t*t * (curve.y2 - curve.ctrly2)

                val angle = atan2(dy, dx)
                val arrowSize = 12

                val miniArrow = Polygon()
                miniArrow.addPoint(x.toInt(), y.toInt())
                miniArrow.addPoint(
                    (x - arrowSize * cos(angle - Math.PI / 6)).toInt(),
                    (y - arrowSize * sin(angle - Math.PI / 6)).toInt()
                )
                miniArrow.addPoint(
                    (x - arrowSize * cos(angle + Math.PI / 6)).toInt(),
                    (y - arrowSize * sin(angle + Math.PI / 6)).toInt()
                )

                g2d.fill(miniArrow)
            }
        }

        private fun drawCard(g2d: Graphics2D, card: ScreenCard, isHovered: Boolean, isDragged: Boolean) {
            val x = card.x
            val y = card.y
            val w = card.width
            val h = card.height

            val shadowOffset = if (isHovered || isDragged) 8 else 4
            g2d.color = Color(0, 0, 0, if (isHovered || isDragged) 40 else 20)
            g2d.fillRoundRect(x + shadowOffset, y + shadowOffset, w, h, 15, 15)


            g2d.color = JBColor(Color(255, 255, 255), Color(45, 45, 45))
            g2d.fillRoundRect(x, y, w, h, 15, 15)

            val headerHeight = 30
            val bgColor = when (card.screen.type) {
                ScreenType.Form -> JBColor(Color(59, 130, 246), Color(59, 130, 246))
                ScreenType.Confirm -> JBColor(Color(245, 158, 11), Color(245, 158, 11))
                ScreenType.Success -> JBColor(Color(34, 197, 94), Color(34, 197, 94))
                ScreenType.List -> JBColor(Color(139, 92, 246), Color(139, 92, 246))
                ScreenType.Empty -> JBColor(Color(107, 114, 128), Color(107, 114, 128))
            }

            g2d.color = if (isHovered || isDragged) bgColor.brighter() else bgColor
            g2d.fillRoundRect(x, y, w, headerHeight, 15, 15)
            g2d.fillRect(x, y + headerHeight - 15, w, 15)


            g2d.color = JBColor(Color(200, 200, 200), Color(60, 60, 60))
            g2d.stroke = BasicStroke(if (isHovered || isDragged) 3f else 2f)
            g2d.drawRoundRect(x, y, w - 1, h - 1, 15, 15)

            g2d.color = Color.WHITE
            g2d.font = Font("SF Pro Display", Font.BOLD, 10)
            val metrics = g2d.fontMetrics
            val nameWidth = metrics.stringWidth(card.screen.name)
            g2d.drawString(card.screen.name, x + (w - nameWidth) / 2, y + 20)

            drawComponents(g2d, card, x, y + headerHeight)
        }

        private fun drawComponents(g2d: Graphics2D, card: ScreenCard, startX: Int, startY: Int) {
            var currentY = startY + 5
            val maxWidth = card.width - 20

            card.screen.components.forEach { component ->
                val definition = ComponentRegistry.getComponentByType(component.type)
                if (definition != null) {
                    try {
                        val icon = definition.getDisplayIcon(null)
                        val scaledIcon = scaleIconForCard(icon, maxWidth)

                        val iconX = startX + (card.width - scaledIcon.iconWidth) / 2
                        g2d.drawImage(scaledIcon.image, iconX, currentY, null)

                        currentY += scaledIcon.iconHeight
                    } catch (e: Exception) {
                    }
                }
            }


            if (card.screen.footerComponents.isNotEmpty()) {
                g2d.color = JBColor(Color(200, 200, 200), Color(60, 60, 60))
                g2d.stroke = BasicStroke(1f)
                g2d.drawLine(startX + 10, currentY, startX + card.width - 10, currentY)
                currentY += 5

                card.screen.footerComponents.forEach { component ->
                    val definition = ComponentRegistry.getComponentByType(component.type)
                    if (definition != null) {
                        try {
                            val icon = definition.getDisplayIcon(null)
                            val scaledIcon = scaleIconForCard(icon, maxWidth)

                            val iconX = startX + (card.width - scaledIcon.iconWidth) / 2
                            g2d.drawImage(scaledIcon.image, iconX, currentY, null)

                            currentY += scaledIcon.iconHeight
                        } catch (e: Exception) {
                        }
                    }
                }
            }
        }

        private fun scaleIconForCard(icon: ImageIcon, maxWidth: Int): ImageIcon {
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
    }

    override fun createActions(): Array<Action> {
        return arrayOf(okAction)
    }

    data class ScreenCard(
        val screen: Screen,
        var x: Int,
        var y: Int,
        val width: Int = 200,
        val height: Int = 350
    )

    data class Connection(
        val source: ScreenCard,
        val target: ScreenCard,
        val label: String?
    )
}