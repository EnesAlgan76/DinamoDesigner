package org.jetbrains.plugins.designer.ui.dialogs

import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import org.jetbrains.plugins.designer.services.LogViewerConfigService
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.awt.geom.RoundRectangle2D
import java.io.File
import java.io.RandomAccessFile
import java.text.SimpleDateFormat
import java.util.Date
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class LogViewerDialog(private val project: Project) : JFrame("Log Viewer") {

    private val configService = LogViewerConfigService.getInstance(project)

    private lateinit var filePathField: JTextField
    private lateinit var logTextPane: JTextPane
    private lateinit var lineGutter: LineGutter
    private lateinit var scrollPane: JScrollPane
    private lateinit var statusLabel: JBLabel

    private var lastFileSize = 0L
    private var currentFile: File? = null

    private val tailTimer = Timer(500) { readNewContent() }
    private val timeFmt = SimpleDateFormat("HH:mm:ss")

    companion object {
        private const val MAX_LINES = 5000
        private const val INITIAL_LINES = 500
        private val MONO_FONT = Font(Font.MONOSPACED, Font.PLAIN, 11)
    }

    init {
        defaultCloseOperation = DISPOSE_ON_CLOSE
        isResizable = true

        val content = JPanel(BorderLayout()).apply {
            background = JBColor(Color(250, 251, 252), Color(43, 45, 48))
        }
        content.add(buildHeader(), BorderLayout.NORTH)
        content.add(buildLogArea(), BorderLayout.CENTER)
        content.add(buildStatusBar(), BorderLayout.SOUTH)

        contentPane = content
        setSize(1000, 700)
        setLocationRelativeTo(null)

        addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent) {
                tailTimer.stop()
            }
        })

        val saved = configService.getLogFilePath()
        if (saved.isNotBlank()) {
            SwingUtilities.invokeLater {
                filePathField.text = saved
                startTailing(saved)
            }
        }
    }

    // ── Header ─────────────────────────────────────────────────────────────────

    private fun buildHeader(): JPanel = JPanel(BorderLayout()).apply {
        isOpaque = false
        border = JBUI.Borders.empty(16, 20, 10, 20)

        add(JBLabel("Log Viewer").apply {
            font = Font("SF Pro Display", Font.BOLD, 18)
            foreground = JBColor(Color(30, 41, 59), Color(241, 245, 249))
        }, BorderLayout.NORTH)

        add(JPanel(BorderLayout(10, 0)).apply {
            isOpaque = false
            border = JBUI.Borders.emptyTop(12)
            val inputPanel = buildRoundedInput()
            inputPanel.add(JLabel("📄").apply { font = font.deriveFont(13f) }, BorderLayout.WEST)
            filePathField = buildFileField()
            inputPanel.add(filePathField, BorderLayout.CENTER)
            add(inputPanel, BorderLayout.CENTER)
        }, BorderLayout.CENTER)
    }

    private fun buildFileField(): JTextField = object : JTextField() {
        init {
            font = MONO_FONT
            background = JBColor(Color(249, 250, 251), Color(55, 58, 64))
            foreground = JBColor(Color(55, 65, 81), Color(209, 213, 219))
            border = JBUI.Borders.empty()
            isOpaque = false
            isEditable = false
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            toolTipText = "Tıkla — log dosyası seç (*.log, *.txt)"

            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    val chooser = JFileChooser().apply {
                        fileSelectionMode = JFileChooser.FILES_ONLY
                        if (text.isNotBlank()) currentDirectory = File(text).parentFile
                        fileFilter = object : javax.swing.filechooser.FileFilter() {
                            override fun accept(f: File) =
                                f.isDirectory || f.name.endsWith(".log") || f.name.endsWith(".txt")
                            override fun getDescription() = "Log dosyaları (*.log, *.txt)"
                        }
                    }
                    if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                        val path = chooser.selectedFile.absolutePath
                        text = path
                        configService.setLogFilePath(path)
                        startTailing(path)
                    }
                }
            })
        }

        override fun paintComponent(g: Graphics) {
            (g as Graphics2D).setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON
            )
            super.paintComponent(g)
        }
    }

    private fun buildRoundedInput(): JPanel = object : JPanel(BorderLayout(10, 0)) {
        init {
            isOpaque = false
            background = JBColor(Color(249, 250, 251), Color(55, 58, 64))
            border = JBUI.Borders.empty(8, 12)
            preferredSize = Dimension(Int.MAX_VALUE, 40)
        }

        override fun paintComponent(g: Graphics) {
            val g2 = g as Graphics2D
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g2.color = background
            g2.fill(RoundRectangle2D.Float(0f, 0f, width.toFloat(), height.toFloat(), 10f, 10f))
            g2.color = JBColor(Color(203, 213, 225), Color(71, 85, 105))
            g2.stroke = BasicStroke(1f)
            g2.draw(RoundRectangle2D.Float(0.5f, 0.5f, width - 1f, height - 1f, 10f, 10f))
            super.paintComponent(g)
        }
    }

    // ── Log area ───────────────────────────────────────────────────────────────

    private fun buildLogArea(): JPanel = JPanel(BorderLayout()).apply {
        isOpaque = false
        border = JBUI.Borders.empty(0, 20, 0, 20)

        logTextPane = JTextPane().apply {
            background = JBColor(Color(250, 251, 252), Color(30, 33, 38))
            foreground = JBColor(Color(30, 41, 59), Color(210, 215, 225))
            font = MONO_FONT
            border = JBUI.Borders.empty(4, 6)
        }

        lineGutter = LineGutter(logTextPane)

        scrollPane = JScrollPane(logTextPane).apply {
            border = BorderFactory.createLineBorder(
                JBColor(Color(218, 222, 232), Color(58, 61, 68)), 1
            )
            setRowHeaderView(lineGutter)
            horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
            verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_ALWAYS
            viewport.addChangeListener { lineGutter.repaint() }
        }

        add(buildToolbar(), BorderLayout.NORTH)
        add(scrollPane, BorderLayout.CENTER)
    }

    // ── Toolbar ────────────────────────────────────────────────────────────────

    private fun buildToolbar(): JPanel = JPanel(FlowLayout(FlowLayout.LEFT, 8, 6)).apply {
        isOpaque = false
        add(iconBtn("🗑  Temizle", Color(239, 68, 68)) { clearLog() })
    }

    private fun iconBtn(label: String, tint: Color, action: () -> Unit): JButton =
        object : JButton(label) {
            private var hover = false

            init {
                font = Font("SF Pro Display", Font.PLAIN, 11)
                foreground = tint
                isContentAreaFilled = false
                isFocusPainted = false
                cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                border = BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color(tint.red, tint.green, tint.blue, 80), 1, true),
                    JBUI.Borders.empty(4, 10)
                )
                addActionListener { action() }
                addMouseListener(object : MouseAdapter() {
                    override fun mouseEntered(e: MouseEvent) { hover = true; repaint() }
                    override fun mouseExited(e: MouseEvent) { hover = false; repaint() }
                })
            }

            override fun paintComponent(g: Graphics) {
                val g2 = g as Graphics2D
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                g2.color = Color(tint.red, tint.green, tint.blue, if (hover) 35 else 12)
                g2.fill(RoundRectangle2D.Float(0f, 0f, width.toFloat(), height.toFloat(), 8f, 8f))
                super.paintComponent(g)
            }
        }

    // ── Status bar ─────────────────────────────────────────────────────────────

    private fun buildStatusBar(): JPanel = JPanel(BorderLayout()).apply {
        isOpaque = false
        border = JBUI.Borders.empty(6, 20, 12, 20)
        statusLabel = JBLabel("Dosya seçilmedi — yukarıdaki alana tıkla").apply {
            font = Font("SF Pro Display", Font.PLAIN, 11)
            foreground = JBColor(Color(107, 114, 128), Color(107, 114, 128))
        }
        add(statusLabel, BorderLayout.WEST)
    }

    // ── Tailing logic ──────────────────────────────────────────────────────────

    private fun startTailing(path: String) {
        tailTimer.stop()
        clearSilent()
        lastFileSize = 0L
        val file = File(path)
        if (!file.exists()) {
            statusLabel.text = "⚠  Dosya bulunamadı: $path"
            return
        }
        currentFile = file
        loadInitial(file)
        statusLabel.text = "📄  ${file.name}  |  İzleniyor…"
        tailTimer.start()
    }

    private fun loadInitial(file: File) {
        try {
            val lines = file.readLines(Charsets.UTF_8)
            val slice = if (lines.size > INITIAL_LINES) lines.takeLast(INITIAL_LINES) else lines
            appendLines(slice)
            lastFileSize = file.length()
        } catch (e: Exception) {
            statusLabel.text = "❌  Okuma hatası: ${e.message}"
        }
    }

    private fun readNewContent() {
        val file = currentFile ?: return
        try {
            val size = file.length()
            when {
                size > lastFileSize -> {
                    val bytes = RandomAccessFile(file, "r").use { raf ->
                        raf.seek(lastFileSize)
                        ByteArray((size - lastFileSize).toInt()).also { raf.readFully(it) }
                    }
                    lastFileSize = size
                    val newLines = String(bytes, Charsets.UTF_8).split("\n").filter { it.isNotBlank() }
                    if (newLines.isNotEmpty()) {
                        SwingUtilities.invokeLater {
                            appendLines(newLines)
                            trimOld()
                            val total = logTextPane.document.defaultRootElement.elementCount
                            statusLabel.text = "📄  ${file.name}  |  ${timeFmt.format(Date())}  |  $total satır"
                        }
                    }
                }
                size < lastFileSize -> {
                    lastFileSize = 0L
                    SwingUtilities.invokeLater {
                        clearSilent()
                        statusLabel.text = "🔄  Dosya yenilendi — ${timeFmt.format(Date())}"
                    }
                }
            }
        } catch (_: Exception) {}
    }

    private fun appendLines(lines: List<String>) {
        val doc = logTextPane.styledDocument
        lines.forEach { line -> doc.insertString(doc.length, "$line\n", null) }
        lineGutter.syncHeight()
    }

    private fun trimOld() {
        val doc = logTextPane.styledDocument
        val root = doc.defaultRootElement
        if (root.elementCount > MAX_LINES) {
            val excess = root.elementCount - MAX_LINES
            doc.remove(0, root.getElement(excess - 1).endOffset)
        }
    }

    private fun clearLog() {
        clearSilent()
        lastFileSize = currentFile?.length() ?: 0L
        statusLabel.text = currentFile?.let { "📄  ${it.name}  |  Ekran temizlendi" } ?: "Dosya seçilmedi"
    }

    private fun clearSilent() {
        val doc = logTextPane.styledDocument
        doc.remove(0, doc.length)
        lineGutter.syncHeight()
    }

    // ── Line number gutter ─────────────────────────────────────────────────────

    inner class LineGutter(private val pane: JTextPane) : JPanel() {

        private val bg = JBColor(Color(242, 244, 247), Color(38, 41, 47))
        private val fg = JBColor(Color(148, 158, 175), Color(95, 110, 130))
        private val fgActive = JBColor(Color(99, 102, 241), Color(147, 150, 255))
        private val hoverBg = JBColor(Color(99, 102, 241, 20), Color(99, 102, 241, 20))
        private val selBg = JBColor(Color(99, 102, 241, 45), Color(99, 102, 241, 45))
        private val sep = JBColor(Color(218, 222, 232), Color(55, 58, 65))

        private var hoveredLine = -1
        private var selectedLine = -1

        init {
            background = bg
            preferredSize = Dimension(50, 0)

            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) = selectLine(e.y)
                override fun mouseExited(e: MouseEvent) { hoveredLine = -1; repaint() }
            })
            addMouseMotionListener(object : MouseMotionAdapter() {
                override fun mouseMoved(e: MouseEvent) {
                    val idx = lineAt(e.y)
                    if (idx != hoveredLine) { hoveredLine = idx; repaint() }
                }
            })

            pane.document.addDocumentListener(object : DocumentListener {
                override fun insertUpdate(e: DocumentEvent) = syncHeight()
                override fun removeUpdate(e: DocumentEvent) = syncHeight()
                override fun changedUpdate(e: DocumentEvent) {}
            })
        }

        fun syncHeight() {
            SwingUtilities.invokeLater {
                val lineCount = pane.document.defaultRootElement.elementCount
                val digits = maxOf(lineCount.toString().length, 3)
                val fm = getFontMetrics(pane.font)
                val w = fm.stringWidth("0".repeat(digits)) + 22
                val h = pane.preferredSize.height
                if (preferredSize.width != w || preferredSize.height != h) {
                    preferredSize = Dimension(w, h)
                    revalidate()
                }
                repaint()
            }
        }

        private fun lineAt(panelY: Int): Int {
            val root = pane.document.defaultRootElement
            for (i in 0 until root.elementCount) {
                try {
                    val y = pane.modelToView2D(root.getElement(i).startOffset).y
                    val nextY = if (i < root.elementCount - 1)
                        pane.modelToView2D(root.getElement(i + 1).startOffset).y
                    else Double.MAX_VALUE
                    if (panelY >= y && panelY < nextY) return i
                } catch (_: Exception) {}
            }
            return -1
        }

        private fun selectLine(panelY: Int) {
            val idx = lineAt(panelY)
            if (idx < 0) return
            selectedLine = idx
            repaint()
            val root = pane.document.defaultRootElement
            val el = root.getElement(idx)
            val end = (el.endOffset - 1).coerceAtLeast(el.startOffset)
            pane.select(el.startOffset, end)
            pane.requestFocusInWindow()
        }

        override fun paintComponent(g: Graphics) {
            val g2 = g as Graphics2D
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

            g2.color = bg
            g2.fillRect(0, 0, width, height)
            g2.color = sep
            g2.drawLine(width - 1, 0, width - 1, height)

            val root = pane.document.defaultRootElement
            val fm = g2.getFontMetrics(pane.font)
            val clip = g2.clipBounds ?: Rectangle(0, 0, width, height)

            for (i in 0 until root.elementCount) {
                try {
                    val y = pane.modelToView2D(root.getElement(i).startOffset).y.toInt()
                    if (y + fm.height + 4 < clip.y) continue
                    if (y > clip.y + clip.height) break

                    val lineH = fm.height + 2
                    val label = (i + 1).toString()
                    val lx = width - fm.stringWidth(label) - 8

                    when (i) {
                        hoveredLine -> {
                            g2.color = hoverBg
                            g2.fillRect(0, y, width - 1, lineH)
                            g2.color = fgActive
                        }
                        selectedLine -> {
                            g2.color = selBg
                            g2.fillRect(0, y, width - 1, lineH)
                            g2.color = fgActive
                        }
                        else -> g2.color = fg
                    }
                    g2.drawString(label, lx, y + fm.ascent + 2)
                } catch (_: Exception) {}
            }
        }
    }
}
