package org.jetbrains.plugins.designer.services

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project

@State(
    name = "LogViewerConfig",
    storages = [Storage("logViewerConfig.xml")]
)
@Service(Service.Level.PROJECT)
class LogViewerConfigService : PersistentStateComponent<LogViewerConfigService.State> {

    data class State(var logFilePath: String = "")

    private var myState = State()

    override fun getState(): State = myState
    override fun loadState(state: State) { myState = state }

    fun getLogFilePath(): String = myState.logFilePath
    fun setLogFilePath(path: String) { myState.logFilePath = path }

    companion object {
        fun getInstance(project: Project): LogViewerConfigService = project.service()
    }
}
