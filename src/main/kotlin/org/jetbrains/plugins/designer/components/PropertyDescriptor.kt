package org.jetbrains.plugins.designer.components

sealed class PropertyDescriptor(val key: String) {
    data class Text(val k: String, val default: String = "") : PropertyDescriptor(k)
    data class Number(val k: String, val default: Int = 0) : PropertyDescriptor(k)
    data class Boolean(val k: String, val default: kotlin.Boolean = false) : PropertyDescriptor(k)
    data class Enum(val k: String, val default: String, val options: List<String>) : PropertyDescriptor(k)
    data class ScreenReference(val k: String) : PropertyDescriptor(k)
}
