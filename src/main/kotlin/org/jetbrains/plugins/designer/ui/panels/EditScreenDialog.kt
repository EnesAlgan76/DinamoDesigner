package org.jetbrains.plugins.designer.ui.panels

import com.intellij.openapi.ui.DialogWrapper
import org.jetbrains.plugins.designer.models.Screen
import org.jetbrains.plugins.designer.models.ScreenType
import java.awt.Dimension
import javax.swing.*

class EditScreenDialog(
    private val screen: Screen,
    private val existingScreens: List<Screen> = emptyList()
) : DialogWrapper(true) {

    private val nameField = JTextField(screen.name)
    private val typeComboBox = JComboBox(ScreenType.values())
    private val descriptionField = JTextField(screen.description)
    private val isEntryScreenCheckBox = JCheckBox("Entry Screen", screen.isEntryScreen)
    private val nextScreenComboBox = JComboBox<String>()

    init {
        title = "Edit Screen"
        typeComboBox.selectedItem = screen.type
        updateNextScreenOptions()

        screen.nextScreenId?.let { nextId ->
            val index = existingScreens.indexOfFirst { it.id == nextId }
            if (index >= 0) {
                nextScreenComboBox.selectedIndex = index
            }
        }

        typeComboBox.addActionListener {
            updateNextScreenOptions()
        }

        init()
    }

    private fun updateNextScreenOptions() {
        nextScreenComboBox.removeAllItems()
        nextScreenComboBox.addItem("-- None --")

        val selectedType = typeComboBox.selectedItem as? ScreenType

        if (selectedType == ScreenType.Form) {
            existingScreens.filter { it.id != screen.id }.forEach { otherScreen ->
                nextScreenComboBox.addItem("${otherScreen.name} (${otherScreen.type})")
            }
        }

        nextScreenComboBox.isEnabled = selectedType == ScreenType.Form
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.preferredSize = Dimension(400, 320)

        panel.add(JLabel("Screen Name:"))
        nameField.preferredSize = Dimension(380, 30)
        panel.add(nameField)
        panel.add(Box.createVerticalStrut(10))

        panel.add(JLabel("Screen Type:"))
        typeComboBox.preferredSize = Dimension(380, 30)
        panel.add(typeComboBox)
        panel.add(Box.createVerticalStrut(10))

        panel.add(JLabel("Description:"))
        descriptionField.preferredSize = Dimension(380, 30)
        panel.add(descriptionField)
        panel.add(Box.createVerticalStrut(10))

        panel.add(JLabel("Next Screen (Form Only):"))
        nextScreenComboBox.preferredSize = Dimension(380, 30)
        panel.add(nextScreenComboBox)
        panel.add(Box.createVerticalStrut(10))

        val checkBoxPanel = JPanel()
        checkBoxPanel.layout = BoxLayout(checkBoxPanel, BoxLayout.X_AXIS)
        checkBoxPanel.add(isEntryScreenCheckBox)
        checkBoxPanel.add(Box.createHorizontalGlue())

        panel.add(checkBoxPanel)
        panel.add(Box.createVerticalStrut(5))

        return panel
    }

    fun getUpdatedScreen(): Screen? {
        val name = nameField.text.trim()
        val type = typeComboBox.selectedItem as ScreenType
        val description = descriptionField.text.trim()
        val isEntryScreen = isEntryScreenCheckBox.isSelected

        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(
                contentPane,
                "Screen name cannot be empty",
                "Validation Error",
                JOptionPane.ERROR_MESSAGE
            )
            return null
        }

        val nextScreenId = if (type == ScreenType.Form && nextScreenComboBox.selectedIndex > 0) {
            val selectedIndex = nextScreenComboBox.selectedIndex - 1
            existingScreens.filter { it.id != screen.id }.getOrNull(selectedIndex)?.id
        } else {
            null
        }

        return screen.copy(
            name = name,
            type = type,
            description = description.ifEmpty { name },
            isEntryScreen = isEntryScreen,
            nextScreenId = nextScreenId
        )
    }
}