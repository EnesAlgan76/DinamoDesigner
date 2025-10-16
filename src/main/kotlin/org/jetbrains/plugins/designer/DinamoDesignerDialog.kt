package org.jetbrains.plugins.designer

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.impl.welcomeScreen.learnIde.coursesInProgress.mainBackgroundColor
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import org.jetbrains.plugins.designer.models.*
import org.jetbrains.plugins.designer.codegen.CodeGenerator
import org.jetbrains.plugins.designer.settings.PluginSettings
import org.jetbrains.plugins.designer.settings.SettingsDialog
import org.jetbrains.plugins.designer.ui.panels.AddScreenDialog
import org.jetbrains.plugins.designer.ui.panels.EditScreenDialog
import org.jetbrains.plugins.template.designer.DesignPersistence
import org.jetbrains.plugins.template.designer.ui.*
import java.awt.*
import javax.swing.*
import javax.swing.plaf.basic.BasicSplitPaneDivider
import javax.swing.plaf.basic.BasicSplitPaneUI
import kotlin.apply

class DinamoDesignerDialog(private val project: Project) : JFrame("EA") {

    private val screenManager = ScreenManager()
    private val componentManager = ComponentManager()
    private val navigationManager = NavigationFlowManager(screenManager)

    private lateinit var screenListPanel: ScreenListPanel
    private lateinit var libraryPanel: ComponentLibraryPanel
    private lateinit var canvasPanel: CanvasPanel
    private lateinit var propertiesPanel: PropertiesPanel
    private lateinit var codePreviewPanel: CodePreviewPanel

    private var selectedComponent: ComponentInstance? = null

    init {
        title = "Dinamo Multi Enes2-Screen Designer"
        setSize(1400, 800)
        contentPane.background = Color(0, 0, 255)
        contentPane = createMainLayout()
        initializeDefaultScreen()
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

            // Screen List Panel
            screenListPanel = ScreenListPanel(
                screenManager = screenManager,
                onScreenSelected = { screen -> onScreenSelected(screen) },
                onAddScreen = { showAddScreenDialog() },
                onEditScreen = { screen -> showEditScreenDialog(screen) }
            )
            add(screenListPanel, BorderLayout.NORTH)

            // Component Library Panel
            libraryPanel = ComponentLibraryPanel()

            val scrollPane = JScrollPane(libraryPanel).apply {
                isOpaque = false
                viewport.isOpaque = false
                border = JBUI.Borders.empty(10, 0, 0, 0)
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

            // Top toolbar
            val toolbar = createCanvasToolbar()
            add(toolbar, BorderLayout.NORTH)

            // Canvas Panel
            canvasPanel = CanvasPanel(
                componentManager = componentManager,
                onComponentSelected = { component -> onComponentSelected(component) }
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

                g2d.color = JBColor(Color(255, 255, 255, 200), Color(40, 40, 40, 200))
                g2d.fillRoundRect(0, 0, width, height, 15, 15)

                g2d.color = JBColor(Color(255, 255, 255, 100), Color(255, 255, 255, 30))
                g2d.drawRoundRect(0, 0, width - 1, height - 1, 15, 15)
            }
        }.apply {
            isOpaque = false
            border = JBUI.Borders.empty(15, 20)

            val titleLabel = StyledLabel("Design Canvas", 14, Font.BOLD)
            add(titleLabel, BorderLayout.WEST)

            val buttonPanel = JPanel(FlowLayout(FlowLayout.RIGHT, 8, 0)).apply {
                isOpaque = false

                add(createToolbarButton("Settings", ModernButtonStyle.SECONDARY) { showSettings() })
                add(createToolbarButton("Export", ModernButtonStyle.SECONDARY) { exportDesign() })
                add(createToolbarButton("Import", ModernButtonStyle.SECONDARY) { importDesign() })
                add(createToolbarButton("Flow", ModernButtonStyle.INFO) { showScreenFlow() })
                add(createToolbarButton("Clear", ModernButtonStyle.DANGER) { clearCanvas() })
            }
            add(buttonPanel, BorderLayout.EAST)
        }
    }

    private fun createToolbarButton(text: String, style: ModernButtonStyle, action: () -> Unit): JButton {
        return ModernButton(text, style, action).apply {
            preferredSize = Dimension(70, 32)
            font = Font("SF Pro Display", Font.BOLD, 11)
        }
    }

    private fun createRightPanel(): JPanel {
        return GlassmorphicPanel().apply {
            preferredSize = Dimension(320, 0)
            border = JBUI.Borders.empty(15)

            val titleLabel = StyledLabel("Properties", 16, Font.BOLD)
            add(titleLabel, BorderLayout.NORTH)

            // Properties Panel
            propertiesPanel = PropertiesPanel(
                onPropertyChanged = { component, key, value ->
                    onPropertyChanged(component, key, value)
                }
            )

            val scrollPane = JScrollPane(propertiesPanel).apply {
                isOpaque = false
                viewport.isOpaque = false
                border = JBUI.Borders.empty(20, 0, 0, 0)
            }
            add(scrollPane, BorderLayout.CENTER)

            // Code Preview Panel
            codePreviewPanel = CodePreviewPanel(
                onGenerateCode = { generateCode() }
            )
            add(codePreviewPanel, BorderLayout.SOUTH)
        }
    }

    // ========== INITIALIZATION ==========

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
        val dialog = AddScreenDialog()

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
        val dialog = EditScreenDialog(screen)

        if (dialog.showAndGet()) {
            val updatedScreen = dialog.getUpdatedScreen()

            if (updatedScreen != null) {
                try {
                    screenManager.updateScreen(updatedScreen)
                    screenListPanel.refreshScreenList()
                    propertiesPanel.setAvailableScreens(screenManager.getAllScreens())

                    // Refresh canvas if this is the selected screen
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
    }

    // ========== COMPONENT OPERATIONS ==========

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

        // If targetScreen changed, update navigation
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
    }

    // ========== ACTIONS ==========

    private fun generateCode(): String {
        val screens = screenManager.getAllScreens()

        if (screens.isEmpty()) {
            return "// No screens to generate. Please add screens first."
        }

        // Get the flow name from the active editor file
        val flowName = getFlowNameFromActiveEditor()

        val code = CodeGenerator.generateFullFlowCode(project, flowName, screens)
        codePreviewPanel.updateCode(code)

        val settings = PluginSettings.getInstance(project)
        if (settings.autoWriteToEditor) {
            writeCodeToActiveEditor(code)
        }

        return code
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

                // Show notification
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

                    screen.components.forEach { component ->
                        val id = component.id.substringAfter("_").toIntOrNull() ?: 0
                    }
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

    // ========== DIALOG ACTIONS ==========

}