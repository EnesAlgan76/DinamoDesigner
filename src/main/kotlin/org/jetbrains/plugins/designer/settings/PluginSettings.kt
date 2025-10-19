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
        var tfMessagesPath: String = "src/com/pozitron/turkiyefinans/core/messages/messages_tr.properties",
        var screenActionImplPath: String = "src/com/pozitron/turkiyefinans/actions/screen/action/ScreenActionImpl.java",
        var autoWriteToEditor: Boolean = false,
        var autoUpdateScreenAction: Boolean = true
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

    var tfMessagesPath: String
        get() = myState.tfMessagesPath
        set(value) {
            myState.tfMessagesPath = value
        }

    var screenActionImplPath: String
        get() = myState.screenActionImplPath
        set(value) {
            myState.screenActionImplPath = value
        }

    var autoWriteToEditor: Boolean
        get() = myState.autoWriteToEditor
        set(value) {
            myState.autoWriteToEditor = value
        }

    var autoUpdateScreenAction: Boolean
        get() = myState.autoUpdateScreenAction
        set(value) {
            myState.autoUpdateScreenAction = value
        }

    companion object {
        fun getInstance(project: Project): PluginSettings {
            return project.service<PluginSettings>()
        }
    }
}
