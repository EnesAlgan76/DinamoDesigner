package org.jetbrains.plugins.designer.ui.dialogs

import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IconLoader
import com.intellij.util.IconUtil
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import org.jetbrains.plugins.designer.services.LogViewerConfigService
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
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
import javax.swing.text.*

class LogViewerDialog(private val project: Project) : JFrame("Log Viewer") {

    private val configService = LogViewerConfigService.getInstance(project)

    private lateinit var filePathField: JTextField
    private lateinit var textPane: JTextPane
    private lateinit var statusLabel: JBLabel

    private var lastFileSize = 0L
    private var currentFile: File? = null
    private var lineCount = 0

    private val tailTimer = Timer(500) { readNewContent() }
    private val timeFmt = SimpleDateFormat("HH:mm:ss")

    private val scheme: EditorColorsScheme
        get() = EditorColorsManager.getInstance().globalScheme

    private val ideFont: Font
        get() = Font(scheme.consoleFontName, Font.PLAIN, scheme.consoleFontSize)

    private val indentWidth: Float by lazy {
        getFontMetrics(ideFont).stringWidth("     ").toFloat()
    }

    companion object {
        private val LOG_PARSE_REGEX   = Regex("""^(\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}[.,]\d{3})\s+(ERROR|WARN|INFO|DEBUG|TRACE)(.*)$""")
        private val STACK_TRACE_REGEX = Regex("""^(\s+at |\s+\.\.\. \d+ more|Caused by:)""")

        private val COLOR_TIMESTAMP = JBColor(Color(0x7B8EAB), Color(0x6B7E99))
        private val COLOR_ERROR  = JBColor(Color(0xC0392B), Color(0xFF6B6B))
        private val COLOR_WARN   = JBColor(Color(0xD35400), Color(0xFFB347))
        private val COLOR_INFO   = JBColor(Color(0x1A6E2E), Color(0x7EC8A4))
        private val COLOR_DEBUG  = JBColor(Color(0x2471A3), Color(0x6CB4E4))
        private val COLOR_TRACE  = JBColor(Color(0x7D3C98), Color(0xC39BD3))
        private val COLOR_STACK  = JBColor(Color(0x909090), Color(0x707070))

        private val BG_PANEL      = JBColor(Color(0xF7F8FA), Color(0x2B2D30))
        private val BG_HEADER     = JBColor(Color(0xFFFFFF), Color(0x313335))
        private val BORDER_COLOR  = JBColor(Color(0xE4E7EB), Color(0x3C3F41))
        private val GUTTER_BG     = JBColor(Color(0xF0F1F3), Color(0x2B2D30))
        private val GUTTER_FG     = JBColor(Color(0xB0B5BC), Color(0x545A60))
        private val GUTTER_BORDER = JBColor(Color(0xE4E7EB), Color(0x3C3F41))
        private val TITLE_COLOR   = JBColor(Color(0x1E2330), Color(0xDFE1E5))
        private val PATH_FG       = JBColor(Color(0x4B5563), Color(0x9DA5B0))
    }

    init {
        defaultCloseOperation = DISPOSE_ON_CLOSE
        isResizable = true

        textPane = object : JTextPane() {
            override fun getScrollableTracksViewportWidth() = true
        }.apply {
            editorKit = WrapEditorKit()
            font = ideFont
            isEditable = true
            background = scheme.defaultBackground
            foreground = scheme.defaultForeground
            border = JBUI.Borders.empty(8, 12)
            caretColor = scheme.defaultForeground
        }

        val content = JPanel(BorderLayout()).apply { background = BG_PANEL }
        content.add(buildHeader(), BorderLayout.NORTH)
        content.add(buildLogArea(), BorderLayout.CENTER)
        content.add(buildStatusBar(), BorderLayout.SOUTH)

        contentPane = content
        setSize(1060, 720)
        setLocationRelativeTo(null)

        addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent) { tailTimer.stop() }
        })

        val saved = configService.getLogFilePath()
        if (saved.isNotBlank()) SwingUtilities.invokeLater {
            filePathField.text = saved
            startTailing(saved)
        }
    }

    // ── Line number gutter ─────────────────────────────────────────────────────

    private inner class LineNumberPanel : JPanel() {
        init {
            background = GUTTER_BG
            preferredSize = Dimension(52, 0)

            textPane.document.addDocumentListener(object : DocumentListener {
                override fun insertUpdate(e: DocumentEvent) = repaint()
                override fun removeUpdate(e: DocumentEvent) = repaint()
                override fun changedUpdate(e: DocumentEvent) = repaint()
            })

            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    val pt = SwingUtilities.convertPoint(this@LineNumberPanel, e.point, textPane)
                    val offset = textPane.viewToModel2D(pt).toInt()
                    val text = textPane.document.getText(0, textPane.document.length)
                    val start = text.lastIndexOf('\n', offset - 1) + 1
                    val end = text.indexOf('\n', offset).let { if (it == -1) textPane.document.length else it }
                    textPane.select(start, end)
                    textPane.requestFocus()
                }
            })
        }

        override fun getPreferredSize() = Dimension(52, textPane.preferredSize.height)

        override fun paintComponent(g: Graphics) {
            super.paintComponent(g)
            val g2 = g as Graphics2D
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

            g2.color = GUTTER_BORDER
            g2.drawLine(width - 1, 0, width - 1, height)

            val clip = g2.clipBounds
            val text = runCatching { textPane.document.getText(0, textPane.document.length) }.getOrElse { return }
            val numFont = ideFont.deriveFont(ideFont.size - 1f)
            val fm = g2.getFontMetrics(numFont)
            g2.font = numFont
            g2.color = GUTTER_FG

            var lineNum = 1
            var offset = 0
            while (offset <= text.length) {
                val rect = runCatching { textPane.modelToView2D(offset)?.bounds }.getOrNull() ?: break
                if (rect.y + rect.height >= clip.y && rect.y <= clip.y + clip.height) {
                    val label = lineNum.toString()
                    g2.drawString(label, width - fm.stringWidth(label) - 6, rect.y + fm.ascent + 1)
                }
                if (rect.y > clip.y + clip.height) break
                val nextNl = text.indexOf('\n', offset)
                if (nextNl == -1) break
                offset = nextNl + 1
                lineNum++
            }
        }
    }

    // ── Header ─────────────────────────────────────────────────────────────────

    private fun buildHeader(): JPanel = object : JPanel(BorderLayout()) {
        override fun paintComponent(g: Graphics) {
            super.paintComponent(g)
            g.color = BORDER_COLOR
            g.drawLine(0, height - 1, width, height - 1)
        }
    }.apply {
        background = BG_HEADER
        border = JBUI.Borders.empty(14, 20, 14, 20)

        // title row: "Log Viewer" left, icon actions right
        val titleRow = JPanel(BorderLayout()).apply {
            isOpaque = false

            add(JBLabel("Log Viewer").apply {
                font = Font("SF Pro Display", Font.BOLD, 16)
                foreground = TITLE_COLOR
            }, BorderLayout.WEST)

            add(JPanel(FlowLayout(FlowLayout.RIGHT, 4, 0)).apply {
                isOpaque = false
                add(buildIconButton("Alta git", "/icons/scrollend.svg") {
                    textPane.caretPosition = textPane.document.length
                })
                add(buildIconButton("Temizle", "/icons/clear.svg") {
                    textPane.text = ""
                    lineCount = 0
                    statusLabel.text = "Temizlendi"
                })
            }, BorderLayout.EAST)
        }
        add(titleRow, BorderLayout.NORTH)

        // file path row below title
        add(JPanel(BorderLayout()).apply {
            isOpaque = false
            border = JBUI.Borders.emptyTop(10)
            add(buildFilePathPill(), BorderLayout.CENTER)
        }, BorderLayout.CENTER)
    }

    private fun buildIconButton(tooltip: String, iconPath: String, action: () -> Unit): JButton =
        object : JButton() {
            private var hovered = false

            init {
                toolTipText = tooltip
                val raw = IconLoader.getIcon(iconPath, LogViewerDialog::class.java)
                icon = IconUtil.scale(raw, null, 20f / raw.iconWidth.coerceAtLeast(1))
                text = null
                isFocusPainted = false
                isContentAreaFilled = false
                isBorderPainted = false
                border = JBUI.Borders.empty(4)
                val size = Dimension(28, 28)
                preferredSize = size
                minimumSize   = size
                maximumSize   = size
                cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)

                addMouseListener(object : MouseAdapter() {
                    override fun mouseEntered(e: MouseEvent) { hovered = true; repaint() }
                    override fun mouseExited(e: MouseEvent)  { hovered = false; repaint() }
                    override fun mouseClicked(e: MouseEvent) { action() }
                })
            }

            override fun paintComponent(g: Graphics) {
                if (hovered) {
                    val g2 = g as Graphics2D
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                    g2.color = JBColor(Color(0, 0, 0, 18), Color(255, 255, 255, 18))
                    g2.fill(RoundRectangle2D.Float(1f, 1f, width - 2f, height - 2f, 7f, 7f))
                }
                super.paintComponent(g)
            }
        }

    private fun buildFilePathPill(): JPanel = object : JPanel(BorderLayout(8, 0)) {
        override fun paintComponent(g: Graphics) {
            val g2 = g as Graphics2D
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g2.color = JBColor(Color(0xF3F4F6), Color(0x3C3F41))
            g2.fill(RoundRectangle2D.Float(0f, 0f, width.toFloat(), height.toFloat(), 8f, 8f))
            g2.color = BORDER_COLOR
            g2.stroke = BasicStroke(1f)
            g2.draw(RoundRectangle2D.Float(0.5f, 0.5f, width - 1f, height - 1f, 8f, 8f))
            super.paintComponent(g)
        }
    }.apply {
        isOpaque = false
        border = JBUI.Borders.empty(7, 12)

        add(JLabel("📄").apply { font = font.deriveFont(12f) }, BorderLayout.WEST)

        filePathField = object : JTextField() {
            override fun paintComponent(g: Graphics) {
                (g as Graphics2D).setRenderingHint(
                    RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON
                )
                super.paintComponent(g)
            }
        }.apply {
            font = ideFont.deriveFont(ideFont.size.toFloat() - 1f)
            foreground = PATH_FG
            background = null
            border = JBUI.Borders.empty()
            isOpaque = false
            isEditable = false
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            toolTipText = "Tıkla — log dosyası seç"
            text = "Log dosyası seçmek için tıkla…"

            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    val chooser = JFileChooser().apply {
                        fileSelectionMode = JFileChooser.FILES_ONLY
                        if (text.isNotBlank()) runCatching { currentDirectory = File(text).parentFile }
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
        add(filePathField, BorderLayout.CENTER)
    }

    // ── Log area ───────────────────────────────────────────────────────────────

    private fun buildLogArea(): JPanel = JPanel(BorderLayout()).apply {
        isOpaque = false
        border = JBUI.Borders.empty(0, 16, 0, 16)

        val gutter = LineNumberPanel()
        val scroll = JScrollPane(textPane).apply {
            border = JBUI.Borders.empty()
            verticalScrollBarPolicy   = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
            horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
            setRowHeaderView(gutter)
            viewport.addChangeListener { gutter.repaint() }
        }
        add(scroll, BorderLayout.CENTER)
    }

    // ── Status bar ─────────────────────────────────────────────────────────────

    private fun buildStatusBar(): JPanel = object : JPanel(BorderLayout()) {
        override fun paintComponent(g: Graphics) {
            super.paintComponent(g)
            g.color = BORDER_COLOR
            g.drawLine(0, 0, width, 0)
        }
    }.apply {
        background = BG_HEADER
        border = JBUI.Borders.empty(6, 20, 8, 20)
        statusLabel = JBLabel("Dosya seçilmedi").apply {
            font = Font("SF Pro Display", Font.PLAIN, 11)
            foreground = JBColor(Color(0x9CA3AF), Color(0x6B7280))
        }
        add(statusLabel, BorderLayout.WEST)
    }

    // ── Tailing ────────────────────────────────────────────────────────────────

    private fun startTailing(path: String) {
        tailTimer.stop()
        textPane.text = ""
        lineCount = 0
        lastFileSize = 0L
        val file = File(path)
        if (!file.exists()) { statusLabel.text = "⚠  Dosya bulunamadı: $path"; return }
        currentFile = file
        loadInitial(file)
        statusLabel.text = "📄  ${file.name}  |  İzleniyor…"
        tailTimer.start()
    }

    private fun loadInitial(file: File) {
        try {
            val lines = file.readLines(Charsets.UTF_8)
            appendLines(if (lines.size > 500) lines.takeLast(500) else lines)
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
                    if (newLines.isNotEmpty()) SwingUtilities.invokeLater {
                        appendLines(newLines)
                        statusLabel.text = "📄  ${file.name}  |  ${timeFmt.format(Date())}  |  $lineCount satır"
                    }
                }
                size < lastFileSize -> {
                    lastFileSize = 0L
                    SwingUtilities.invokeLater {
                        textPane.text = ""; lineCount = 0
                        statusLabel.text = "🔄  Dosya yenilendi — ${timeFmt.format(Date())}"
                    }
                }
            }
        } catch (_: Exception) {}
    }

    // ── Rendering ──────────────────────────────────────────────────────────────

    private fun appendLines(lines: List<String>) {
        val doc = textPane.styledDocument
        lines.forEach { line ->
            val insertPos = doc.length
            val match = LOG_PARSE_REGEX.find(line)
            if (match != null) {
                val (timestamp, level, rest) = match.destructured
                doc.insertString(doc.length, timestamp,            charAttrs(COLOR_TIMESTAMP))
                doc.insertString(doc.length, " ${level.padEnd(5)}", charAttrs(levelColor(level)))
                doc.insertString(doc.length, "$rest\n",            charAttrs(normalColor()))
            } else {
                val color = if (STACK_TRACE_REGEX.containsMatchIn(line)) COLOR_STACK else normalColor()
                doc.insertString(doc.length, "$line\n", charAttrs(color))
            }
            doc.setParagraphAttributes(insertPos, doc.length - insertPos, hangingIndentAttrs(), false)
            lineCount++
        }
    }

    private fun charAttrs(color: Color): SimpleAttributeSet = SimpleAttributeSet().also {
        StyleConstants.setFontFamily(it, scheme.consoleFontName)
        StyleConstants.setFontSize(it, scheme.consoleFontSize)
        StyleConstants.setForeground(it, color)
    }

    private fun normalColor() = scheme.defaultForeground

    private fun hangingIndentAttrs(): SimpleAttributeSet = SimpleAttributeSet().also {
        val w = indentWidth
        StyleConstants.setLeftIndent(it, w)
        StyleConstants.setFirstLineIndent(it, -w)
    }

    private fun levelColor(level: String): Color = when (level) {
        "ERROR" -> COLOR_ERROR
        "WARN"  -> COLOR_WARN
        "INFO"  -> COLOR_INFO
        "DEBUG" -> COLOR_DEBUG
        "TRACE" -> COLOR_TRACE
        else    -> normalColor()
    }
}

// ── Word-wrap editor kit ──────────────────────────────────────────────────────

private class WrapEditorKit : StyledEditorKit() {
    private val factory = WrapViewFactory()
    override fun getViewFactory(): ViewFactory = factory
}

private class WrapViewFactory : ViewFactory {
    override fun create(elem: Element): View = when (elem.name) {
        AbstractDocument.ContentElementName   -> WrapLabelView(elem)
        AbstractDocument.ParagraphElementName -> ParagraphView(elem)
        AbstractDocument.SectionElementName   -> BoxView(elem, View.Y_AXIS)
        StyleConstants.ComponentElementName   -> ComponentView(elem)
        StyleConstants.IconElementName        -> IconView(elem)
        else                                  -> LabelView(elem)
    }
}

private class WrapLabelView(elem: Element) : LabelView(elem) {
    override fun getMinimumSpan(axis: Int) = if (axis == View.X_AXIS) 0f else super.getMinimumSpan(axis)
}
