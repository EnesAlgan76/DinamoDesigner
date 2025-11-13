package org.jetbrains.plugins.designer.services

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project

@State(
    name = "BackendConfigService",
    storages = [Storage("backendConfig.xml")]
)
@Service(Service.Level.PROJECT)
class BackendConfigService : PersistentStateComponent<BackendConfigService.State> {

    data class State(
        var propertiesFilePath: String = ""
    )

    private var myState = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
    }

    fun getPropertiesFilePath(): String = myState.propertiesFilePath

    fun setPropertiesFilePath(path: String) {
        myState.propertiesFilePath = path
    }

    fun getCurrentChangeNumber(): String? {
        val path = myState.propertiesFilePath
        if (path.isBlank()) return null

        return try {
            val file = java.io.File(path)
            if (!file.exists()) return null

            val content = file.readText()
            val regex = Regex("Dev-(\\d+)")
            regex.find(content)?.groupValues?.get(1)
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        fun getInstance(project: Project): BackendConfigService {
            return project.service()
        }
    }
}
