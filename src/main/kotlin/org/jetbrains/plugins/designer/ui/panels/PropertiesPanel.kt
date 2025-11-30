package org.jetbrains.plugins.template.designer.ui

import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import org.jetbrains.plugins.designer.models.ComponentInstance
import org.jetbrains.plugins.designer.models.Screen
import org.jetbrains.plugins.template.designer.components.ComponentRegistry
import org.jetbrains.plugins.designer.components.PropertyDescriptor
import java.awt.*
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import javax.swing.*

class PropertiesPanel(
    private val onPropertyChanged: (ComponentInstance, String, Any) -> Unit
) : JPanel() {

    private var currentComponent: ComponentInstance? = null
    private var currentScreen: Screen? = null
    private var allScreens: List<Screen> = emptyList()

    fun setAvailableScreens(screens: List<Screen>) {
        allScreens = screens
    }

    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        isOpaque = false
        border = JBUI.Borders.empty(20, 0, 0, 0)

        showEmptyState()
    }

    fun showComponentProperties(component: ComponentInstance, screen: Screen) {
        currentComponent = component
        currentScreen = screen

        removeAll()

        val definition = ComponentRegistry.getComponentByType(component.type)
        val identifier = component.properties["identifier"] as? String ?: component.id

        // Main identifier title
        val titleLabel = StyledLabel(identifier, 14, Font.BOLD).apply {
            alignmentX = Component.CENTER_ALIGNMENT
        }
        add(titleLabel)

        // Small component type subtitle
        val subtitleLabel = StyledLabel(definition?.displayName ?: component.type, 10, Font.PLAIN).apply {
            foreground = JBColor(Color(100, 116, 139), Color(148, 163, 184))
            alignmentX = Component.CENTER_ALIGNMENT
        }
        add(subtitleLabel)
        add(Box.createVerticalStrut(15))

        definition?.propertyDescriptors?.forEach { descriptor ->
            when (descriptor) {
                is PropertyDescriptor.ConditionalGroup -> {
                    add(createConditionalGroupField(descriptor, component))
                    add(Box.createVerticalStrut(10))
                }
                else -> {
                    val value = component.properties[descriptor.key] ?: descriptor.default
                    add(createPropertyField(descriptor.key, value, component))
                    add(Box.createVerticalStrut(10))
                }
            }
        }

        revalidate()
        repaint()
    }

    fun showEmptyState() {
        removeAll()

        val infoLabel = StyledLabel(
            "<html><i>Select a component<br/>to edit properties</i></html>",
            12,
            Font.PLAIN
        ).apply {
            foreground = JBColor(Color(100, 116, 139), Color(148, 163, 184))
        }

        add(infoLabel)
        revalidate()
        repaint()
    }

    fun clearProperties() {
        showEmptyState()
    }

    private fun createPropertyField(key: String, value: Any, component: ComponentInstance): JPanel {
        val definition = ComponentRegistry.getComponentByType(component.type)
        val descriptor = definition?.propertyDescriptors?.find { it.key == key }

        return JPanel(BorderLayout()).apply {
            isOpaque = false
            maximumSize = Dimension(Integer.MAX_VALUE, 70)
            border = JBUI.Borders.empty(5)

            val label = JLabel(key.uppercase()).apply {
                font = Font("SF Pro Display", Font.BOLD, 10)
                foreground = JBColor(Color(100, 116, 139), Color(148, 163, 184))
            }
            add(label, BorderLayout.NORTH)

            val inputField = when (descriptor) {
                is PropertyDescriptor.ConditionalGroup -> {
                    return createConditionalGroupField(descriptor, component)
                }
                is PropertyDescriptor.Boolean -> createBooleanProperty(key, value as kotlin.Boolean, component)
                is PropertyDescriptor.Enum -> createEnumProperty(key, value.toString(), component, descriptor.options)
                is PropertyDescriptor.ScreenReference -> createScreenSelectionProperty(key, value.toString(), component, currentScreen?.let { listOf(it) } ?: emptyList())
                is PropertyDescriptor.Number -> createNumberProperty(key, value, component)
                else -> createTextFieldProperty(key, value.toString(), component)
            }

            add(inputField, BorderLayout.CENTER)
        }
    }


    private fun createEnumProperty(
        key: String,
        value: String,
        component: ComponentInstance,
        options: List<String>
    ): JComboBox<String> {
        return JComboBox<String>().apply {
            options.forEach { addItem(it) }
            selectedItem = value

            addActionListener {
                val selected = selectedItem as? String
                if (selected != null) {
                    onPropertyChanged(component, key, selected)
                }
            }
        }
    }

    private fun createTextFieldProperty(key: String, value: String, component: ComponentInstance): JTextField {
        return JTextField(value).apply {
            border = JBUI.Borders.customLine(JBColor.border(), 1)
            addActionListener {
                onPropertyChanged(component, key, text)
            }
            addFocusListener(object : FocusAdapter() {
                override fun focusLost(e: FocusEvent?) {
                    onPropertyChanged(component, key, text)
                }
            })
        }
    }

    private fun createBooleanProperty(key: String, value: Boolean, component: ComponentInstance): JCheckBox {
        return JCheckBox("", value).apply {
            addActionListener {
                onPropertyChanged(component, key, isSelected)
            }
        }
    }

    private fun createNumberProperty(key: String, value: Any, component: ComponentInstance): JTextField {
        val numValue = when (value) {
            is Number -> value.toString()
            is String -> value
            else -> "0"
        }

        return JTextField(numValue).apply {
            border = JBUI.Borders.customLine(JBColor.border(), 1)
            addActionListener {
                val intValue = text.toIntOrNull() ?: 0
                onPropertyChanged(component, key, intValue)
            }
            addFocusListener(object : FocusAdapter() {
                override fun focusLost(e: FocusEvent?) {
                    val intValue = text.toIntOrNull() ?: 0
                    onPropertyChanged(component, key, intValue)
                }
            })

        }
    }

    private fun createScreenSelectionProperty(
        key: String,
        value: String,
        component: ComponentInstance,
        availableScreens: List<Screen>
    ): JComboBox<String> {
        return JComboBox<String>().apply {
            addItem("No navigation")

            allScreens.filter { it.id != currentScreen?.id }.forEach { screen ->
                addItem("${screen.name} (${screen.type})")
            }

            selectedItem = if (value.isNotEmpty()) {
                allScreens.find { it.id == value }?.let { "${it.name} (${it.type})" } ?: "No navigation"
            } else "No navigation"

            addActionListener {
                val selected = selectedItem as? String
                val targetScreen = allScreens.find { "${it.name} (${it.type})" == selected }
                onPropertyChanged(component, key, targetScreen?.id ?: "")
            }
        }
    }

    private fun createConditionalGroupField(
        descriptor: PropertyDescriptor.ConditionalGroup,
        component: ComponentInstance
    ): JPanel {
        val toggleValue = component.properties[descriptor.toggleKey] as? kotlin.Boolean ?: descriptor.d

        return JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            isOpaque = false
            maximumSize = Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE)
            border = JBUI.Borders.empty(5)

            val childPanel = JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                isOpaque = false
                border = JBUI.Borders.emptyLeft(20)
                isVisible = toggleValue

                descriptor.childProperties.forEach { childDesc ->
                    val childValue = component.properties[childDesc.key] ?: childDesc.default
                    add(createPropertyField(childDesc.key, childValue, component))
                    add(Box.createVerticalStrut(8))
                }
            }

            val headerPanel = JPanel(BorderLayout()).apply {
                isOpaque = false
                maximumSize = Dimension(Integer.MAX_VALUE, 70)
                border = JBUI.Borders.empty(5)

                val label = JLabel(descriptor.k.uppercase()).apply {
                    font = Font("SF Pro Display", Font.BOLD, 10)
                    foreground = JBColor(Color(100, 116, 139), Color(148, 163, 184))
                }
                add(label, BorderLayout.NORTH)

                val toggleCheckbox = JCheckBox("", toggleValue).apply {
                    border = JBUI.Borders.customLine(JBColor.border(), 1)
                    addActionListener {
                        onPropertyChanged(component, descriptor.toggleKey, isSelected)
                        childPanel.isVisible = isSelected
                        revalidate()
                        repaint()
                    }
                }
                add(toggleCheckbox, BorderLayout.CENTER)
            }



            add(headerPanel)
            add(childPanel)
        }
    }
}
