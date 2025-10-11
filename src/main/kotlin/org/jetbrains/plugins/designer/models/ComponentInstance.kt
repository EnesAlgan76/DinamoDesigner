package org.jetbrains.plugins.designer.models

data class ComponentInstance(
    val id: String,
    val type: String,
    val properties: MutableMap<String, Any>
)
