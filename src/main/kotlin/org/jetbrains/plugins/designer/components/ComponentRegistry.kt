package org.jetbrains.plugins.template.designer.components

import org.jetbrains.plugins.designer.components.AmountFieldComponent

object ComponentRegistry {

    private val registeredComponents = listOf(
        TextFieldComponent,
        AmountFieldComponent,
        ComboBoxComponent,
        DatePickerComponent,
        ButtonComponent,
        PaymentToolComponent,
        CheckBoxComponent
    )

    fun getAllComponents(): List<ComponentDefinition> {
        return registeredComponents
    }

    fun getComponentByType(type: String): ComponentDefinition? {
        return registeredComponents.find { it.type == type }
    }

    fun getComponentTypes(): List<String> {
        return registeredComponents.map { it.type }
    }

    fun isValidComponentType(type: String): Boolean {
        return registeredComponents.any { it.type == type }
    }

    fun getComponentByDisplayName(displayName: String): ComponentDefinition? {
        return registeredComponents.find {
            it.displayName.equals(displayName, ignoreCase = true)
        }
    }

    fun getComponentCount(): Int {
        return registeredComponents.size
    }
}