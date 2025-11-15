package org.jetbrains.plugins.designer

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.wm.impl.welcomeScreen.learnIde.coursesInProgress.mainBackgroundColor
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import org.jetbrains.plugins.designer.models.*
import org.jetbrains.plugins.designer.codegen.CodeGenerator
import org.jetbrains.plugins.designer.settings.PluginSettings
import org.jetbrains.plugins.designer.settings.SettingsDialog
import org.jetbrains.plugins.designer.services.PreviewServerManager
import org.jetbrains.plugins.designer.services.EmulatorLauncher
import org.jetbrains.plugins.designer.services.GitHubEmulatorInstaller
import org.jetbrains.plugins.designer.ui.panels.AddScreenDialog
import org.jetbrains.plugins.designer.ui.panels.EditScreenDialog
import org.jetbrains.plugins.template.designer.DesignPersistence
import org.jetbrains.plugins.template.designer.ui.*
import java.awt.*
import java.io.File
import javax.swing.*
import javax.swing.plaf.basic.BasicSplitPaneDivider
import javax.swing.plaf.basic.BasicSplitPaneUI

class DinamoDesignerDialog(private val project: Project) : JFrame("EA") {

    private val screenManager = ScreenManager()
    private val componentManager = ComponentManager()
    private val emulatorInstaller = GitHubEmulatorInstaller(project)
    private val emulatorLauncher = EmulatorLauncher(project)
    private val navigationManager = NavigationFlowManager(screenManager)
    private val previewServerManager = PreviewServerManager.getInstance(project)

    private lateinit var screenListPanel: ScreenListPanel
    private lateinit var libraryPanel: ComponentLibraryPanel
    private lateinit var canvasPanel: CanvasPanel
    private lateinit var propertiesPanel: PropertiesPanel
    private lateinit var codePreviewPanel: CodePreviewPanel

    private var selectedComponent: ComponentInstance? = null

    init {
        title = "Dinamo Multi Enes2-Screen Designer"
        setSize(1400, 800)
        checkSdkAndOfferSetup()
        contentPane.background = Color(0, 0, 255)
        contentPane = createMainLayout()
        initializeDefaultScreen()
        startPreviewServer()
    }

    private fun startPreviewServer() {
        val started = previewServerManager.startServer("10.141.8.82", 8887)
        if (started) {
            println("✅ Preview server started successfully on 10.141.8.82:8887")
            println(previewServerManager.getStatus())
        } else {
            println("❌ Failed to start preview server")
        }
    }




    private fun createMainLayout(): JPanel {
        return GradientPanel.create().apply {
            preferredSize = Dimension(1400, 800)

            val splitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT).apply {
                dividerLocation = 360
                leftComponent = createLeftPanel()
                rightComponent = createCenterAndRightPanel()
                isContinuousLayout = true
                isOpaque = false
                border = null
                dividerSize = 1

                ui = object : BasicSplitPaneUI() {
                    override fun createDefaultDivider(): BasicSplitPaneDivider {
                        return object : BasicSplitPaneDivider(this) {
                            override fun paint(g: Graphics) {
                                g.color = Color(60, 63, 65)
                                g.fillRect(0, 0, size.width, size.height)
                            }
                        }
                    }
                }
            }

            add(splitPane, BorderLayout.CENTER)
        }
    }

    private fun createLeftPanel(): JPanel {
        return GlassmorphicPanel().apply {
            preferredSize = Dimension(360, 0)
            border = JBUI.Borders.empty(15)

            screenListPanel = ScreenListPanel(
                screenManager = screenManager,
                onScreenSelected = { screen -> onScreenSelected(screen) },
                onAddScreen = { showAddScreenDialog() },
                onEditScreen = { screen -> showEditScreenDialog(screen) }
            )
            add(screenListPanel, BorderLayout.NORTH)

            libraryPanel = ComponentLibraryPanel()

            val scrollPane = JScrollPane(libraryPanel).apply {
                isOpaque = false
                viewport.isOpaque = false
                border = JBUI.Borders.empty(10, 0, 0, 0)
                verticalScrollBar.unitIncrement = 16
                verticalScrollBar.blockIncrement = 64
            }
            add(scrollPane, BorderLayout.CENTER)
        }
    }

    private fun createCenterAndRightPanel(): JSplitPane {
        return JSplitPane(JSplitPane.HORIZONTAL_SPLIT).apply {
            dividerLocation = 750
            leftComponent = createCanvasArea()
            rightComponent = createRightPanel()
            isContinuousLayout = true
            isOpaque = false
            border = null
            dividerSize = 1

            ui = object : BasicSplitPaneUI() {
                override fun createDefaultDivider(): BasicSplitPaneDivider {
                    return object : BasicSplitPaneDivider(this) {
                        override fun paint(g: Graphics) {
                            g.color = Color(60, 63, 65)
                            g.fillRect(0, 0, size.width, size.height)
                        }
                    }
                }
            }
        }
    }

    private fun createCanvasArea(): JPanel {
        return JPanel(BorderLayout()).apply {
            isOpaque = false
            border = JBUI.Borders.empty(0, 15, 0, 10)

            val toolbar = createCanvasToolbar()
            add(toolbar, BorderLayout.NORTH)

            canvasPanel = CanvasPanel(
                componentManager = componentManager,
                onComponentSelected = { component -> onComponentSelected(component) },
                onComponentAdded = { screen ->sendPreviewToClients(screen)},
                onComponentDeleted = {screen -> sendPreviewToClients(screen)}
            )

            val scrollPane = JScrollPane(canvasPanel).apply {
                border = JBUI.Borders.empty(15, 0, 0, 0)
                isOpaque = false
                viewport.isOpaque = false
            }
            add(scrollPane, BorderLayout.CENTER)
        }
    }

    private fun createCanvasToolbar(): JPanel {
        return object : JPanel(BorderLayout()) {
            override fun paintComponent(g: Graphics) {
                val g2d = g as Graphics2D
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

                g2d.color = JBColor(Color(255, 255, 255, 240), Color(30, 30, 32, 240))
                g2d.fillRoundRect(0, 0, width, height, 20, 20)

                g2d.color = JBColor(Color(0, 0, 0, 10), Color(0, 0, 0, 30))
                g2d.drawRoundRect(0, 0, width - 1, height - 1, 20, 20)
            }
        }.apply {
            isOpaque = false
            border = JBUI.Borders.empty(12, 24)

            val iconBar = JPanel().apply {
                isOpaque = false
                layout = FlowLayout(FlowLayout.CENTER, 8, 0)

                add(createModernIconButton("/icons/ai.svg", "AI", Color(147, 51, 234)) { showAIGeneration() })
                add(createModernIconButton("/icons/flow.svg", "Run", Color(34, 197, 94)) { runEmulator() })
                add(createModernIconButton("/icons/flow.svg", "Flow", Color(59, 130, 246)) { showScreenFlow() })
                add(createModernIconButton("/icons/setting.svg", "Settings", Color(100, 116, 139)) { showSettings() })
                add(createModernIconButton("/icons/export.svg", "Export", Color(100, 116, 139)) { exportDesign() })
                add(createModernIconButton("/icons/import.svg", "Import", Color(100, 116, 139)) { importDesign() })
                add(createModernIconButton("/icons/clear.svg", "Clear", Color(239, 68, 68)) { clearCanvas() })
            }
            add(iconBar, BorderLayout.CENTER)
        }
    }

    private fun createModernIconButton(iconPath: String, label: String, accentColor: Color, action: () -> Unit): JPanel {
        return JPanel().apply {
            layout = BorderLayout()
            isOpaque = false
            preferredSize = Dimension(64, 60)
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)

            var isHovered = false

            val iconButton = object : JLabel() {
                init {
                    horizontalAlignment = SwingConstants.CENTER
                    preferredSize = Dimension(64, 40)

                    try {
                        val loadedIcon = IconLoader.getIcon(iconPath, DinamoDesignerDialog::class.java)
                        if (loadedIcon != null) {
                            val scaledIcon = com.intellij.util.IconUtil.scale(loadedIcon, null, 20f / loadedIcon.iconWidth)
                            icon = scaledIcon
                        }
                    } catch (e: Exception) {
                        println("⚠️ Icon not found: $iconPath")
                    }
                }

                override fun paintComponent(g: Graphics) {
                    val g2d = g as Graphics2D
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

                    if (isHovered) {
                        g2d.color = Color(accentColor.red, accentColor.green, accentColor.blue, 20)
                        g2d.fillRoundRect(12, 4, width - 24, height - 8, 20, 20)
                    }

                    super.paintComponent(g)
                }
            }

            val textLabel = JLabel(label).apply {
                horizontalAlignment = SwingConstants.CENTER
                font = Font("SF Pro Display", Font.PLAIN, 10)
                foreground = JBColor(Color(100, 116, 139), Color(148, 163, 184))
            }

            add(iconButton, BorderLayout.CENTER)
            add(textLabel, BorderLayout.SOUTH)

            val mouseAdapter = object : java.awt.event.MouseAdapter() {
                override fun mouseEntered(e: java.awt.event.MouseEvent) {
                    isHovered = true
                    textLabel.foreground = accentColor
                    iconButton.repaint()
                }

                override fun mouseExited(e: java.awt.event.MouseEvent) {
                    isHovered = false
                    textLabel.foreground = JBColor(Color(100, 116, 139), Color(148, 163, 184))
                    iconButton.repaint()
                }

                override fun mouseClicked(e: java.awt.event.MouseEvent) {
                    action()
                }
            }

            iconButton.addMouseListener(mouseAdapter)
            textLabel.addMouseListener(mouseAdapter)
            addMouseListener(mouseAdapter)
        }
    }

    private fun createRightPanel(): JPanel {
        return GlassmorphicPanel().apply {
            preferredSize = Dimension(320, 0)
            border = JBUI.Borders.empty(15)

            val titleLabel = StyledLabel("Properties", 16, Font.BOLD)
            add(titleLabel, BorderLayout.NORTH)

            propertiesPanel = PropertiesPanel(
                onPropertyChanged = { component, key, value ->
                    onPropertyChanged(component, key, value)
                }
            )

            val scrollPane = JScrollPane(propertiesPanel).apply {
                isOpaque = false
                viewport.isOpaque = false
                border = JBUI.Borders.empty(20, 0, 0, 0)
                verticalScrollBar.unitIncrement = 16
                verticalScrollBar.blockIncrement = 64
            }
            add(scrollPane, BorderLayout.CENTER)

            codePreviewPanel = CodePreviewPanel(
                onGenerateCode = {
                    val flowCode = generateCode()
                    val screenActionBlock = generateScreenActionBlock()
                    Pair(flowCode, screenActionBlock)
                }
            )
            add(codePreviewPanel, BorderLayout.SOUTH)
        }
    }

    private fun initializeDefaultScreen() {
        val defaultScreen = Screen(
            id = "screen_${System.currentTimeMillis()}",
            name = "MAIN_FORM",
            type = ScreenType.Form,
            description = "Main form screen",
            isEntryScreen = true
        )

        screenManager.addScreen(defaultScreen)
        screenManager.selectScreen(defaultScreen.id)
        screenListPanel.refreshScreenList()
        propertiesPanel.setAvailableScreens(screenManager.getAllScreens())
        onScreenSelected(defaultScreen)
    }


    private fun showAddScreenDialog() {
        val dialog = AddScreenDialog(screenManager.getAllScreens())

        if (dialog.showAndGet()) {
            val screen = dialog.getScreen()

            if (screen != null) {
                try {
                    screenManager.addScreen(screen)
                    screenListPanel.refreshScreenList()

                    screenManager.selectScreen(screen.id)
                    canvasPanel.loadScreen(screen)
                    propertiesPanel.clearProperties()
                    propertiesPanel.setAvailableScreens(screenManager.getAllScreens())

                } catch (e: Exception) {
                    JOptionPane.showMessageDialog(
                        null,
                        "Error adding screen: ${e.message}",
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                    )
                }
            }
        }
    }

    private fun showEditScreenDialog(screen: Screen) {
        val dialog = EditScreenDialog(screen, screenManager.getAllScreens())

        if (dialog.showAndGet()) {
            val updatedScreen = dialog.getUpdatedScreen()

            if (updatedScreen != null) {
                try {
                    screenManager.updateScreen(updatedScreen)
                    screenListPanel.refreshScreenList()
                    propertiesPanel.setAvailableScreens(screenManager.getAllScreens())

                    if (screenManager.getSelectedScreen()?.id == updatedScreen.id) {
                        canvasPanel.loadScreen(updatedScreen)
                    }

                } catch (e: Exception) {
                    JOptionPane.showMessageDialog(
                        null,
                        "Error updating screen: ${e.message}",
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                    )
                }
            }
        }
    }


    private fun onScreenSelected(screen: Screen) {
        screenManager.selectScreen(screen.id)
        screenListPanel.refreshScreenList()
        canvasPanel.loadScreen(screen)
        propertiesPanel.showEmptyState()
        propertiesPanel.setAvailableScreens(screenManager.getAllScreens())
        selectedComponent = null

        sendPreviewToClients(screen)
    }

    private fun onComponentSelected(component: ComponentInstance) {
        selectedComponent = component
        val selectedScreen = screenManager.getSelectedScreen()

        if (selectedScreen != null) {
            propertiesPanel.showComponentProperties(component, selectedScreen)
        }
    }

    private fun onPropertyChanged(component: ComponentInstance, key: String, value: Any) {
        val selectedScreen = screenManager.getSelectedScreen() ?: return

        componentManager.updateComponentProperty(component.id, key, value, selectedScreen)
        canvasPanel.refreshComponents()

        if (key == "targetScreen" && value is String) {
            if (value.isNotEmpty()) {
                navigationManager.addConnection(
                    selectedScreen.id,
                    value,
                    component.id,
                    null
                )
            } else {
                navigationManager.removeConnection(component.id)
            }
        }

        sendPreviewToClients(selectedScreen)
    }

    private fun sendPreviewToClients(screen: Screen) {
        try {
            previewServerManager.sendPreview(screen)
        } catch (e: Exception) {
            println("⚠️ Failed to send preview: ${e.message}")
        }
    }


    private fun generateCode(): String {
        val screens = screenManager.getAllScreens()

        if (screens.isEmpty()) {
            return "No screens to generate. Please add screens first."
        }

        val flowName = getFlowNameFromActiveEditor()

        val code = CodeGenerator.generateFullFlowCode(project, flowName, screens)

        val settings = PluginSettings.getInstance(project)
        if (settings.autoWriteToEditor) {
            writeCodeToActiveEditor(code)
        }

        return code
    }

    private fun generateScreenActionBlock(): String {
        val screens = screenManager.getAllScreens()

        if (screens.isEmpty()) {
            return "No screens available"
        }

        val flowName = getFlowNameFromActiveEditor()
        val screenActionBlock = CodeGenerator.generateScreenActionImplBlock(project, flowName, screens)
        return screenActionBlock
    }


    private fun getFlowNameFromActiveEditor(): String {
        val editorManager = FileEditorManager.getInstance(project)
        val selectedEditor = editorManager.selectedTextEditor

        if (selectedEditor != null) {
            val virtualFile = FileDocumentManager.getInstance().getFile(selectedEditor.document)
            if (virtualFile != null) {
                val fileName = virtualFile.nameWithoutExtension
                if (fileName.isNotEmpty()) {
                    return fileName
                }
            }
        }
        return "TFGeneratedFlow"
    }

    private fun writeCodeToActiveEditor(code: String) {
        ApplicationManager.getApplication().invokeLater {
            val editorManager = FileEditorManager.getInstance(project)
            val selectedEditor = editorManager.selectedTextEditor

            if (selectedEditor != null) {
                val document = selectedEditor.document

                WriteCommandAction.runWriteCommandAction(project) {
                    document.setText(code)

                    val psiDocumentManager = PsiDocumentManager.getInstance(project)
                    psiDocumentManager.commitDocument(document)

                    val psiFile = psiDocumentManager.getPsiFile(document)
                    if (psiFile != null) {
                        try {
                            val codeStyleManager = JavaCodeStyleManager.getInstance(project)
                            codeStyleManager.optimizeImports(psiFile)
                            codeStyleManager.shortenClassReferences(psiFile)
                        } catch (e: Exception) {
                        }
                    }
                }

                JOptionPane.showMessageDialog(
                    contentPane,
                    "Generated code has been written to the active editor",
                    "Code Generated",
                    JOptionPane.INFORMATION_MESSAGE
                )
            } else {
                JOptionPane.showMessageDialog(
                    contentPane,
                    "No active editor found. Please open a file first.",
                    "Auto-write Failed",
                    JOptionPane.WARNING_MESSAGE
                )
            }
        }
    }

    private fun exportDesign() {
        val screens = screenManager.getAllScreens()
        DesignPersistence.exportDesign(screens, contentPane)
    }

    private fun importDesign() {
        val importedScreens = DesignPersistence.importDesign(contentPane)

        if (importedScreens != null && importedScreens.isNotEmpty()) {
            val result = JOptionPane.showConfirmDialog(
                contentPane,
                "Replace current design with ${importedScreens.size} imported screens?",
                "Confirm Import",
                JOptionPane.YES_NO_OPTION
            )

            if (result == JOptionPane.YES_OPTION) {
                screenManager.clearAll()
                componentManager.resetCounter()

                importedScreens.forEach { screen ->
                    screenManager.addScreen(screen)
                }

                screenListPanel.refreshScreenList()

                if (importedScreens.isNotEmpty()) {
                    onScreenSelected(importedScreens[0])
                }
            }
        }
    }

    private fun showScreenFlow() {
        val screens = screenManager.getAllScreens()

        if (screens.isEmpty()) {
            JOptionPane.showMessageDialog(
                contentPane,
                "No screens to visualize",
                "Screen Flow",
                JOptionPane.INFORMATION_MESSAGE
            )
            return
        }

        SwingUtilities.invokeLater {
            val flowDialog = ScreenFlowVisualizerDialog(project, screens)
            flowDialog.show()
        }
    }

    private fun clearCanvas() {
        val result = JOptionPane.showConfirmDialog(
            contentPane,
            "Clear all components from current screen?",
            "Clear Canvas",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        )

        if (result == JOptionPane.YES_OPTION) {
            val selectedScreen = screenManager.getSelectedScreen()
            if (selectedScreen != null) {
                selectedScreen.components.clear()
                canvasPanel.clearCanvas()
                propertiesPanel.showEmptyState()
            }
        }
    }

    private fun showSettings() {
        val settingsDialog = SettingsDialog(project)
        settingsDialog.show()
    }

    private fun showAIGeneration() {
        val selectedScreen = screenManager.getSelectedScreen()

        if (selectedScreen == null) {
            JOptionPane.showMessageDialog(
                contentPane,
                "Lütfen önce bir ekran seçin",
                "AI Generation",
                JOptionPane.WARNING_MESSAGE
            )
            return
        }

        val aiDialog = AIGenerationDialog(project)

        if (aiDialog.showAndGet()) {
            val generatedJson = aiDialog.getGeneratedJson()

            if (generatedJson != null) {
                try {
                    if (!org.jetbrains.plugins.designer.services.ComponentJsonParser.validateJson(generatedJson)) {
                        throw IllegalArgumentException("Invalid JSON format")
                    }

                    val addedCount = org.jetbrains.plugins.designer.services.ComponentJsonParser.parseAndAddComponents(
                        generatedJson,
                        selectedScreen,
                        componentManager
                    )

                    canvasPanel.loadScreen(selectedScreen)
                    propertiesPanel.showEmptyState()

                    sendPreviewToClients(selectedScreen)

                    JOptionPane.showMessageDialog(
                        contentPane,
                        "$addedCount component başarıyla eklendi!",
                        "AI Generation",
                        JOptionPane.INFORMATION_MESSAGE
                    )

                } catch (e: Exception) {
                    JOptionPane.showMessageDialog(
                        contentPane,
                        "Component oluşturma hatası: ${e.message}",
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                    )
                }
            }
        }
    }


    private fun runEmulator() {
        // Check if emulator is installed (from GitHub or system)
        var emulatorPath = emulatorInstaller.getEmulatorPath()

     // if (emulatorPath == null) {
     //     // Try system SDK
     //     val sdkPath = findAndroidSdkPath()
     //     if (sdkPath != null) {
     //         emulatorPath = getEmulatorPath(sdkPath)
     //     }
     // }

        if (emulatorPath == null) {
            val result = JOptionPane.showConfirmDialog(
                contentPane,
                "Emulator not found.\nWould you like to download it from GitHub?",
                "Emulator Required",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
            )

            if (result == JOptionPane.YES_OPTION) {
                installEmulatorFromGitHub()
            }
            return
        }

        val finalEmulatorPath = emulatorPath

        // Use default AVD directly - no dialog needed
        val defaultAvdName = "Dinamo_Pixel_5"

        // Show progress dialog
        val progressDialog = JDialog(this, "Launching Emulator", true)
        val progressLabel = JLabel("Starting emulator with $defaultAvdName...")
        val progressBar = JProgressBar().apply { isIndeterminate = true }

        progressDialog.apply {
            layout = BorderLayout(10, 10)
            add(progressLabel, BorderLayout.NORTH)
            add(progressBar, BorderLayout.CENTER)
            setSize(400, 100)
            setLocationRelativeTo(this@DinamoDesignerDialog)
        }

        // Launch in background
        Thread {
            emulatorLauncher.launchEmulatorWithApp(
                                avdName = defaultAvdName,
                                apkPath = null,
                                packageName = null,
                                activityName = null,
                onSuccess = {
                    SwingUtilities.invokeLater {
                        progressDialog.dispose()
                        JOptionPane.showMessageDialog(
                            contentPane,
                            "Emulator started successfully!",
                            "Success",
                            JOptionPane.INFORMATION_MESSAGE
                        )
                    }
                },
                onError = { error ->
                    SwingUtilities.invokeLater {
                        progressDialog.dispose()
                        JOptionPane.showMessageDialog(
                            contentPane,
                            "Error: $error",
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                        )
                    }
                }
            )
        }.start()

        progressDialog.isVisible = true
    }

    private fun showRunConfigDialog(avds: List<String>): RunConfig? {
        val panel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = JBUI.Borders.empty(10)
        }

        // AVD Selection
        panel.add(JLabel("Select AVD:"))
        val avdCombo = JComboBox(avds.toTypedArray())
        panel.add(avdCombo)
        panel.add(Box.createVerticalStrut(15))

        // APK Path
        panel.add(JLabel("APK Path (optional):"))
        val apkField = JTextField(30)
        val apkPanel = JPanel(BorderLayout(5, 0)).apply {
            add(apkField, BorderLayout.CENTER)
            add(JButton("Browse").apply {
                addActionListener {
                    val fileChooser = JFileChooser().apply {
                        fileFilter = javax.swing.filechooser.FileNameExtensionFilter("APK Files", "apk")
                    }
                    if (fileChooser.showOpenDialog(panel) == JFileChooser.APPROVE_OPTION) {
                        apkField.text = fileChooser.selectedFile.absolutePath
                    }
                }
            }, BorderLayout.EAST)
        }
        panel.add(apkPanel)
        panel.add(Box.createVerticalStrut(10))

        // Package Name
        panel.add(JLabel("Package Name (e.g., com.example.app):"))
        val packageField = JTextField(30)
        panel.add(packageField)
        panel.add(Box.createVerticalStrut(10))

        // Activity Name
        panel.add(JLabel("Main Activity (e.g., .MainActivity):"))
        val activityField = JTextField(30)
        panel.add(activityField)
        panel.add(Box.createVerticalStrut(10))

        panel.add(JLabel("<html><i>Leave APK fields empty to just start the emulator</i></html>").apply {
            foreground = JBColor.GRAY
        })

        val result = JOptionPane.showConfirmDialog(
            contentPane,
            panel,
            "Run Configuration",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        )

        if (result == JOptionPane.OK_OPTION) {
            val avdName = avdCombo.selectedItem?.toString() ?: return null
            val apkPath = apkField.text.takeIf { it.isNotBlank() }
            val packageName = packageField.text.takeIf { it.isNotBlank() }
            val activityName = activityField.text.takeIf { it.isNotBlank() }

            return RunConfig(avdName, apkPath, packageName, activityName)
        }

        return null
    }

    private data class RunConfig(
        val avdName: String,
        val apkPath: String?,
        val packageName: String?,
        val activityName: String?
    )

    private fun checkSdkAndOfferSetup() {
        // Check on startup if emulator is installed
        if (!emulatorInstaller.isEmulatorInstalled()) {
            SwingUtilities.invokeLater {
                val result = JOptionPane.showConfirmDialog(
                    contentPane,
                    """
                    Android Emulator not found!
                    
                    Would you like to install the emulator now?
                    It will be installed to: ~/dinamoemulator
                    
                    This is completely independent from your system Android installation.
                    
                    (You can also do this later from the Run button)
                    """.trimIndent(),
                    "Setup Emulator",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
                )

                if (result == JOptionPane.YES_OPTION) {
                    installEmulatorFromGitHub()
                }
            }
        }
    }

    private fun installEmulatorFromGitHub() {
        emulatorInstaller.installEmulator { success, message ->
            SwingUtilities.invokeLater {
                if (success) {
                    JOptionPane.showMessageDialog(
                        contentPane,
                        "Emulator installed successfully!\nLocation: $message",
                        "Installation Complete",
                        JOptionPane.INFORMATION_MESSAGE
                    )
                } else {
                    JOptionPane.showMessageDialog(
                        contentPane,
                        "Installation failed: $message",
                        "Installation Error",
                        JOptionPane.ERROR_MESSAGE
                    )
                }
            }
        }
    }


    private fun listAvailableAvds(emulatorPath: String): List<String> {
        try {
            // First try to use emulator -list-avds command
            val processBuilder = ProcessBuilder(emulatorPath, "-list-avds")

            // Set environment to use our custom AVD location
            val sdkPath = "/Users/enesalgan/dinamoemulator"
            processBuilder.environment()["ANDROID_AVD_HOME"] = "$sdkPath/dinamoemulator/avd"
            processBuilder.environment()["ANDROID_SDK_ROOT"] = "$sdkPath/dinamoemulator"

            val process = processBuilder.start()
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()

            val avdList = output.lines()
                .filter { it.isNotBlank() }
                .map { it.trim() }

            // If emulator command doesn't work, manually scan our AVD directory
            if (avdList.isEmpty()) {
                val avdDir = File("$sdkPath/avd")
                if (avdDir.exists()) {
                    return avdDir.listFiles()
                        ?.filter { it.name.endsWith(".ini") && it.name != "Dinamo_Pixel_5.ini" }
                        ?.map { it.name.substringBeforeLast(".ini") }
                        ?: listOf("Dinamo_Pixel_5") // Return default if nothing found
                }
            }

            return avdList
        } catch (_: Exception) {
            // If all fails, return the default AVD we create
            return listOf("Dinamo_Pixel_5")
        }
    }


}