package org.jetbrains.plugins.designer.models

class ScreenManager {
    private val screens = mutableListOf<Screen>()
    private var selectedScreenId: String? = null

    fun addScreen(screen: Screen) {
        if (screens.any { it.id == screen.id }) {
            throw IllegalArgumentException("Screen with id ${screen.id} already exists")
        }
        if (screens.isEmpty() && !screen.isEntryScreen) {
            screens.add(screen.copy(isEntryScreen = true))
        } else {
            screens.add(screen)
        }
    }

    fun removeScreen(screenId: String): Boolean {
        val screen = screens.find { it.id == screenId } ?: return false

        // Don't allow removing the last screen
        if (screens.size == 1) {
            return false
        }

        val removed = screens.remove(screen)

        // If removed screen was selected, select first screen
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
}
