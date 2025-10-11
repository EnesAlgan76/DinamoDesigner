package org.jetbrains.plugins.template.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiManager
import org.jetbrains.plugins.template.MyBundle

@Service(Service.Level.PROJECT)
class MyProjectService(private val project: Project) : FileEditorManagerListener {

    private val analyzer = IfStatementAnalyzerService(project)
    private val listeners = mutableListOf<IfCounterListener>()
    private var currentResult: IfAnalysisResult? = null

    init {
        thisLogger().info(MyBundle.message("projectService", project.name))
        project.messageBus.connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, this)
    }

    fun addListener(listener: IfCounterListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: IfCounterListener) {
        listeners.remove(listener)
    }

    private fun notifyListeners() {
        val statements = currentResult?.statements ?: emptyList()
        val fileName = currentResult?.fileName ?: ""
        listeners.forEach { it.onIfStatementsChanged(statements, fileName) }
    }

    override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
        updateIfStatements(file)
    }

    override fun selectionChanged(event: FileEditorManagerEvent) {
        event.newFile?.let { updateIfStatements(it) }
    }

    fun updateIfStatements(file: VirtualFile?) {
        if (file == null || !file.name.endsWith(".java")) {
            currentResult = null
            notifyListeners()
            return
        }

        val psiFile = PsiManager.getInstance(project).findFile(file)
        currentResult = if (psiFile is PsiJavaFile) {
            analyzer.analyze(psiFile)
        } else {
            null
        }

        notifyListeners()
    }

    fun checkCurrentFile() {
        val fileEditorManager = FileEditorManager.getInstance(project)
        val selectedFiles = fileEditorManager.selectedFiles
        if (selectedFiles.isNotEmpty()) {
            updateIfStatements(selectedFiles[0])
        }
    }

    fun generateJsonStructure(): String {
        return currentResult?.formatAsJson() ?: "// No analysis available"
    }

    fun getCurrentIfStatements(): List<IfStatementInfo> {
        return currentResult?.statements ?: emptyList()
    }

    fun getCurrentFileName(): String {
        return currentResult?.fileName ?: ""
    }

    fun getCurrentIfCount(): Int {
        return currentResult?.totalCount ?: 0
    }

    interface IfCounterListener {
        fun onIfStatementsChanged(statements: List<IfStatementInfo>, fileName: String)
    }
}