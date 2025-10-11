package org.jetbrains.plugins.designer.settings

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
@State(
    name = "DinamoDesignerSettings",
    storages = [Storage("DinamoDesignerSettings.xml")]
)
class PluginSettings : PersistentStateComponent<PluginSettings.State> {

    private var myState = State()

    data class State(
        var tfIdentifierPath: String = "src/com/pozitron/turkiyefinans/core/TFIdentifier.java",
        var autoWriteToEditor: Boolean = false
    )

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
    }

    var tfIdentifierPath: String
        get() = myState.tfIdentifierPath
        set(value) {
            myState.tfIdentifierPath = value
        }

    var autoWriteToEditor: Boolean
        get() = myState.autoWriteToEditor
        set(value) {
            myState.autoWriteToEditor = value
        }

    companion object {
        fun getInstance(project: Project): PluginSettings {
            return project.service<PluginSettings>()
        }
    }
}
