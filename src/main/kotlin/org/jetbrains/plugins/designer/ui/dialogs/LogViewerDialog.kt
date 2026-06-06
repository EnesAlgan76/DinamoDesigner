package org.jetbrains.plugins.designer.ui.dialogs

import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
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

class LogViewerDialog(private val project: Project) : JFrame("Log Viewer") {

    private val configService = LogViewerConfigService.getInstance(project)

    private lateinit var filePathField: JTextField
    private lateinit var consoleView: ConsoleView
    private lateinit var statusLabel: JBLabel

    private var lastFileSize = 0L
    private var currentFile: File? = null
    private var lineCount = 0

    private val tailTimer = Timer(500) { readNewContent() }
    private val timeFmt = SimpleDateFormat("HH:mm:ss")

    companion object {
        private val MONO_FONT = Font(Font.MONOSPACED, Font.PLAIN, 11)
        private val LOG_LEVEL_REGEX = Regex("""^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}[.,]\d{3}\s+(ERROR|WARN|INFO|DEBUG|TRACE)""")
        private val STACK_TRACE_REGEX = Regex("""^(\s+at |\s+\.\.\. \d+ more|Caused by:)""")
    }

    init {
        defaultCloseOperation = DISPOSE_ON_CLOSE
        isResizable = true

        consoleView = TextConsoleBuilderFactory.getInstance().createBuilder(project).console

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
                consoleView.dispose()
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

        val consoleActions = DefaultActionGroup(*consoleView.createConsoleActions())
        val toolbar = ActionManager.getInstance()
            .createActionToolbar("LogViewerConsole", consoleActions, true)
        toolbar.targetComponent = consoleView.component

        add(toolbar.component, BorderLayout.NORTH)
        add(consoleView.component, BorderLayout.CENTER)
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
        consoleView.clear()
        lineCount = 0
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
            val slice = if (lines.size > 500) lines.takeLast(500) else lines
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
                            statusLabel.text = "📄  ${file.name}  |  ${timeFmt.format(Date())}  |  $lineCount satır"
                        }
                    }
                }
                size < lastFileSize -> {
                    lastFileSize = 0L
                    SwingUtilities.invokeLater {
                        consoleView.clear()
                        lineCount = 0
                        statusLabel.text = "🔄  Dosya yenilendi — ${timeFmt.format(Date())}"
                    }
                }
            }
        } catch (_: Exception) {}
    }

    private fun appendLines(lines: List<String>) {
        lines.forEach { line ->
            consoleView.print("$line\n", contentTypeFor(line))
            lineCount++
        }
    }

    private fun contentTypeFor(line: String): ConsoleViewContentType {
        val match = LOG_LEVEL_REGEX.find(line)
        if (match != null) {
            return when (match.groupValues[1]) {
                "ERROR" -> ConsoleViewContentType.LOG_ERROR_OUTPUT
                "WARN"  -> ConsoleViewContentType.LOG_WARNING_OUTPUT
                "INFO"  -> ConsoleViewContentType.LOG_INFO_OUTPUT
                "DEBUG" -> ConsoleViewContentType.LOG_DEBUG_OUTPUT
                "TRACE" -> ConsoleViewContentType.LOG_VERBOSE_OUTPUT
                else    -> ConsoleViewContentType.NORMAL_OUTPUT
            }
        }
        return if (STACK_TRACE_REGEX.containsMatchIn(line))
            ConsoleViewContentType.LOG_DEBUG_OUTPUT
        else
            ConsoleViewContentType.NORMAL_OUTPUT
    }
}
