package org.jetbrains.plugins.designer.models

class ScreenManager {
    private val screens = mutableListOf<Screen>()
    private var selectedScreenId: String? = null

    fun addScreen(screen: Screen) {
        if (screens.any { it.id == screen.id }) {
            throw IllegalArgumentException("Screen with id ${screen.id} already exists")
        }

        val screenToAdd = if (screens.isEmpty() && !screen.isEntryScreen) {
            screen.copy(isEntryScreen = true)
        } else {
            screen
        }

        // Add default continue button for Form screens
        if (screenToAdd.type == ScreenType.Form && screenToAdd.footerComponents.isEmpty()) {
            val continueButton = ComponentInstance(
                id = "continue_button_${screenToAdd.id}",
                type = "BUTTON",
                properties = mutableMapOf(
                    "identifier" to "continueButton",
                    "text" to "Continue",
                    "targetScreen" to "",
                    "isContinueButton" to true
                )
            )
            screenToAdd.footerComponents.add(continueButton)
        }

        screens.add(screenToAdd)
    }

    fun removeScreen(screenId: String): Boolean {
        val screen = screens.find { it.id == screenId } ?: return false

        if (screens.size == 1) {
            return false
        }

        val removed = screens.remove(screen)

        if (selectedScreenId == screenId && screens.isNotEmpty()) {
            selectedScreenId = screens[0].id
        }

        return removed
    }

    fun getScreen(screenId: String): Screen? {
        return screens.find { it.id == screenId }
    }

    fun getAllScreens(): List<Screen> {
        return screens.toList()
    }

    fun selectScreen(screenId: String) {
        if (screens.any { it.id == screenId }) {
            selectedScreenId = screenId
        }
    }

    fun getSelectedScreen(): Screen? {
        return selectedScreenId?.let { getScreen(it) }
    }

    fun clearAll() {
        screens.clear()
        selectedScreenId = null
    }

    fun getScreenCount(): Int = screens.size

    fun hasScreens(): Boolean = screens.isNotEmpty()

    fun getScreensByType(type: ScreenType): List<Screen> {
        return screens.filter { it.type == type }
    }

    fun updateScreen(updatedScreen: Screen): Boolean {
        val index = screens.indexOfFirst { it.id == updatedScreen.id }
        if (index == -1) return false

        screens[index] = updatedScreen
        return true
    }
}
