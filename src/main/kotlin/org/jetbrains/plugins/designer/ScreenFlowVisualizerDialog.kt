package org.jetbrains.plugins.designer

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.JBColor
import org.jetbrains.plugins.designer.models.Screen
import org.jetbrains.plugins.designer.models.ScreenType
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.geom.CubicCurve2D
import javax.swing.*
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Screen Flow Visualizer - Ekranlar arası navigasyon akışını görselleştirir
 * HTML'deki Connected Cards benzeri bir deneyim sunar
 */
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
        // Create cards for each screen
        screens.forEachIndexed { index, screen ->
            val x = 150 + (index % 4) * 250
            val y = 150 + (index / 4) * 200
            val card = ScreenCard(screen, x, y)
            cards.add(card)
        }

        // Create connections based on navigation
        screens.forEach { screen ->
            // 1. Next Screen bağlantıları (Form ekranlar için default continue button)
            if (screen.type == ScreenType.Form && screen.nextScreenId != null) {
                val sourceCard = cards.find { it.screen.id == screen.id }
                val targetCard = cards.find { it.screen.id == screen.nextScreenId }

                if (sourceCard != null && targetCard != null) {
                    connections.add(Connection(sourceCard, targetCard, "Continue"))
                }
            }

            // 2. Button component bağlantıları
            screen.components.forEach { component ->
                val targetScreenId = component.properties["targetScreen"] as? String
                if (!targetScreenId.isNullOrEmpty()) {
                    val sourceCard = cards.find { it.screen.id == screen.id }
                    val targetCard = cards.find { it.screen.id == targetScreenId }

                    if (sourceCard != null && targetCard != null) {
                        // Button label'ı varsa onu kullan
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

        init {
            background = JBColor(Color(240, 242, 247), Color(30, 31, 34))
            preferredSize = Dimension(1200, 700)

            setupMouseListeners()
        }

        private fun setupMouseListeners() {
            val mouseAdapter = object : MouseAdapter() {
                override fun mousePressed(e: MouseEvent) {
                    val card = findCardAt(e.x, e.y)
                    if (card != null) {
                        draggedCard = card
                        dragOffsetX = e.x - card.x
                        dragOffsetY = e.y - card.y
                        cursor = Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR)
                    }
                }

                override fun mouseReleased(e: MouseEvent) {
                    draggedCard = null
                    cursor = Cursor.getDefaultCursor()
                }

                override fun mouseDragged(e: MouseEvent) {
                    draggedCard?.let { card ->
                        card.x = (e.x - dragOffsetX).coerceIn(0, width - card.width)
                        card.y = (e.y - dragOffsetY).coerceIn(0, height - card.height)
                        repaint()
                    }
                }

                override fun mouseMoved(e: MouseEvent) {
                    val newHovered = findCardAt(e.x, e.y)
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
            }

            addMouseListener(mouseAdapter)
            addMouseMotionListener(mouseAdapter)
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

            // Draw connections first (below cards)
            connections.forEach { connection ->
                drawConnection(g2d, connection)
            }

            // Draw cards
            cards.forEach { card ->
                val isHovered = card == hoveredCard
                val isDragged = card == draggedCard
                drawCard(g2d, card, isHovered, isDragged)
            }

            // Draw info panel
            drawInfoPanel(g2d)
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
                JBColor(Color(46, 204, 113, 180), Color(46, 204, 113, 180))  // Yeşil
            } else {
                JBColor(Color(52, 152, 219, 180), Color(52, 152, 219, 180))  // Mavi
            }

            // Ana çizgi - Dashed pattern ile yön gösterme
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
            val arrowSize = 16  // 12'den 16'ya çıkardık

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
                JBColor(Color(39, 174, 96), Color(39, 174, 96))  // Koyu yeşil
            } else {
                JBColor(Color(41, 128, 185), Color(41, 128, 185))  // Koyu mavi
            }
            g2d.color = arrowBorderColor
            g2d.draw(arrow)

            drawDirectionArrows(g2d, curve, isContinueConnection)
        }

        private fun drawDirectionArrows(g2d: Graphics2D, curve: CubicCurve2D.Double, isContinue: Boolean = false) {
            val numArrows = 2
            val arrowColor = if (isContinue) {
                JBColor(Color(46, 204, 113, 200), Color(46, 204, 113, 200))  // Yeşil
            } else {
                JBColor(Color(52, 152, 219, 200), Color(52, 152, 219, 200))  // Mavi
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

            // Shadow
            val shadowOffset = if (isHovered || isDragged) 8 else 4
            g2d.color = Color(0, 0, 0, if (isHovered || isDragged) 40 else 20)
            g2d.fillRoundRect(x + shadowOffset, y + shadowOffset, w, h, 15, 15)

            // Card background
            val bgColor = when (card.screen.type) {
                ScreenType.Form -> JBColor(Color(59, 130, 246), Color(59, 130, 246))
                ScreenType.Confirm -> JBColor(Color(245, 158, 11), Color(245, 158, 11))
                ScreenType.Success -> JBColor(Color(34, 197, 94), Color(34, 197, 94))
                ScreenType.List -> JBColor(Color(139, 92, 246), Color(139, 92, 246))
                ScreenType.Empty -> JBColor(Color(107, 114, 128), Color(107, 114, 128))
            }

            g2d.color = if (isHovered || isDragged) bgColor.brighter() else bgColor
            g2d.fillRoundRect(x, y, w, h, 15, 15)

            // Border
            g2d.color = Color(255, 255, 255, if (isHovered || isDragged) 150 else 80)
            g2d.stroke = BasicStroke(if (isHovered || isDragged) 3f else 2f)
            g2d.drawRoundRect(x, y, w - 1, h - 1, 15, 15)

            // Text
            g2d.color = Color.WHITE
            g2d.font = Font("SF Pro Display", Font.BOLD, 14)

            val metrics = g2d.fontMetrics
            val nameWidth = metrics.stringWidth(card.screen.name)
            g2d.drawString(card.screen.name, x + (w - nameWidth) / 2, y + 30)

            g2d.font = Font("SF Pro Display", Font.PLAIN, 11)
            g2d.color = Color(255, 255, 255, 180)
            val typeText = card.screen.type.name
            val typeWidth = metrics.stringWidth(typeText)
            g2d.drawString(typeText, x + (w - typeWidth) / 2, y + 50)

            // Component count
            g2d.font = Font("SF Pro Display", Font.PLAIN, 10)
            val componentCount = "${card.screen.components.size} components"
            val countWidth = metrics.stringWidth(componentCount)
            g2d.drawString(componentCount, x + (w - countWidth) / 2, y + h - 15)
        }

        private fun drawInfoPanel(g2d: Graphics2D) {
            // Stats panel
            g2d.color = JBColor(Color(255, 255, 255, 220), Color(40, 40, 40, 220))
            g2d.fillRoundRect(20, 20, 200, 100, 12, 12)

            g2d.color = JBColor.foreground()
            g2d.font = Font("SF Pro Display", Font.BOLD, 14)
            g2d.drawString("📊 Statistics", 35, 45)

            g2d.font = Font("SF Pro Display", Font.PLAIN, 12)
            g2d.drawString("Screens: ${cards.size}", 35, 70)
            g2d.drawString("Connections: ${connections.size}", 35, 90)

            // Controls panel
            g2d.color = JBColor(Color(255, 255, 255, 220), Color(40, 40, 40, 220))
            g2d.fillRoundRect(width - 220, 20, 200, 80, 12, 12)

            g2d.color = JBColor.foreground()
            g2d.font = Font("SF Pro Display", Font.BOLD, 14)
            g2d.drawString("🎮 Controls", width - 205, 45)

            g2d.font = Font("SF Pro Display", Font.PLAIN, 11)
            g2d.drawString("• Drag cards to move", width - 205, 65)
            g2d.drawString("• Hover for details", width - 205, 82)
        }
    }

    override fun createActions(): Array<Action> {
        return arrayOf(okAction)
    }

    data class ScreenCard(
        val screen: Screen,
        var x: Int,
        var y: Int,
        val width: Int = 180,
        val height: Int = 100
    )

    data class Connection(
        val source: ScreenCard,
        val target: ScreenCard,
        val label: String?
    )
}