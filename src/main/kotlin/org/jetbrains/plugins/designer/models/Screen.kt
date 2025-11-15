package org.jetbrains.plugins.designer.models

data class Screen(
    val id: String,
    val name: String,
    val type: ScreenType,
    val description: String,
    val components: MutableList<ComponentInstance> = mutableListOf(),
    val isEntryScreen : Boolean,
    val footerComponents: MutableList<ComponentInstance> = mutableListOf()
)
