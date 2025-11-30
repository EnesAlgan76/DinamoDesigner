package org.jetbrains.plugins.designer.components

sealed class PropertyDescriptor(val key: String, val default: Any) {
    data class Text(val k: String, val d: String = "") : PropertyDescriptor(k, d)
    data class Number(val k: String, val d: Int = 0) : PropertyDescriptor(k, d)
    data class Boolean(val k: String, val d: kotlin.Boolean = false) : PropertyDescriptor(k, d)
    data class Enum(val k: String, val d: String, val options: List<String>) : PropertyDescriptor(k, d)
    data class ScreenReference(val k: String, val d: String = "") : PropertyDescriptor(k, d)
    data class ConditionalGroup(
        val k: String,
        val toggleKey: String,
        val d: kotlin.Boolean = false,
        val childProperties: List<PropertyDescriptor>
    ) : PropertyDescriptor(k, d)
}
