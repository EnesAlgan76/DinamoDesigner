package org.jetbrains.plugins.designer.ui.panels

import com.intellij.openapi.ui.DialogWrapper
import org.jetbrains.plugins.designer.models.Screen
import org.jetbrains.plugins.designer.models.ScreenType
import java.awt.Dimension
import java.util.*
import javax.swing.*

class AddScreenDialog(private val existingScreens: List<Screen> = emptyList()) : DialogWrapper(true) {

    private val nameField = JTextField()
    private val typeComboBox = JComboBox(ScreenType.values())
    private val descriptionField = JTextField()
    private val isEntryScreenCheckBox = JCheckBox("Entry Screen", false)
    private val nextScreenComboBox = JComboBox<String>()

    init {
        title = "Add New Screen"
        updateNextScreenOptions()

        // Screen Type değiştiğinde Next Screen seçeneklerini güncelle
        typeComboBox.addActionListener {
            updateNextScreenOptions()
        }

        init()
    }

    private fun updateNextScreenOptions() {
        nextScreenComboBox.removeAllItems()
        nextScreenComboBox.addItem("-- None --")

        val selectedType = typeComboBox.selectedItem as? ScreenType

        // Sadece Form ekranlar için next screen seçeneği göster
        if (selectedType == ScreenType.Form) {
            existingScreens.forEach { screen ->
                nextScreenComboBox.addItem("${screen.name} (${screen.type})")
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

        // Next Screen seçeneği (sadece Form ekranlar için)
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

        // Next Screen seçimini al (sadece Form ekranlar için)
        val nextScreenId = if (type == ScreenType.Form && nextScreenComboBox.selectedIndex > 0) {
            val selectedIndex = nextScreenComboBox.selectedIndex - 1  // "-- None --" offset'i
            existingScreens.getOrNull(selectedIndex)?.id
        } else {
            null
        }

        return Screen(
            id = UUID.randomUUID().toString(),
            name = name,
            type = type,
            description = description.ifEmpty { name },
            isEntryScreen = isEntryScreen,
            nextScreenId = nextScreenId
        )
    }
}