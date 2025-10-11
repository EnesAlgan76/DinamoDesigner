package org.jetbrains.plugins.designer.ui.panels

import com.intellij.openapi.ui.DialogWrapper
import org.jetbrains.plugins.designer.models.Screen
import org.jetbrains.plugins.designer.models.ScreenType
import java.awt.Dimension
import java.util.*
import javax.swing.*

class AddScreenDialog : DialogWrapper(true) {

    private val nameField = JTextField()
    private val typeComboBox = JComboBox(ScreenType.values())
    private val descriptionField = JTextField()
    private val isEntryScreenCheckBox = JCheckBox("Entry Screen", false)

    init {
        title = "Add New Screen"
        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.preferredSize = Dimension(400, 250)

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

        val checkBoxPanel = JPanel()
        checkBoxPanel.layout = BoxLayout(checkBoxPanel, BoxLayout.X_AXIS)
        checkBoxPanel.add(isEntryScreenCheckBox)
        checkBoxPanel.add(Box.createHorizontalGlue())

        panel.add(checkBoxPanel)
        panel.add(Box.createVerticalStrut(5))

        return panel
    }

    fun getScreen(): Screen? {
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

        return Screen(
            id = UUID.randomUUID().toString(),
            name = name,
            type = type,
            description = description.ifEmpty { name },
            isEntryScreen = isEntryScreen
        )
    }
}