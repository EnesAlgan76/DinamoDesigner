package org.jetbrains.plugins.designer.settings

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.*

class SettingsDialog(private val project: Project) : DialogWrapper(project) {

    private val tfIdentifierPathField = JBTextField(40)
    private val autoWriteCheckBox = JBCheckBox("Auto-write generated code to active editor")
    private val settings = PluginSettings.getInstance(project)

    init {
        title = "Dinamo Designer Settings"
        tfIdentifierPathField.text = settings.tfIdentifierPath
        autoWriteCheckBox.isSelected = settings.autoWriteToEditor
        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(GridBagLayout())
        panel.border = JBUI.Borders.empty(10)

        val gbc = GridBagConstraints()
        gbc.gridx = 0
        gbc.gridy = 0
        gbc.anchor = GridBagConstraints.WEST
        gbc.insets = JBUI.insets(5)

        // TFIdentifier Path Label
        val pathLabel = JBLabel("TFIdentifier Path:")
        panel.add(pathLabel, gbc)

        gbc.gridx = 1
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.weightx = 1.0
        panel.add(tfIdentifierPathField, gbc)

        // Add description
        gbc.gridx = 1
        gbc.gridy = 1
        gbc.insets = JBUI.insets(0, 5, 5, 5)
        val description = JLabel("<html><i>Relative path from project root to TFIdentifier.java</i></html>")
        description.foreground = JBUI.CurrentTheme.ContextHelp.FOREGROUND
        panel.add(description, gbc)

        // Auto-write checkbox
        gbc.gridx = 0
        gbc.gridy = 2
        gbc.gridwidth = 2
        gbc.insets = JBUI.insets(15, 5, 5, 5)
        panel.add(autoWriteCheckBox, gbc)

        // Checkbox description
        gbc.gridy = 3
        gbc.insets = JBUI.insets(0, 25, 5, 5)
        val checkboxDesc = JLabel("<html><i>When enabled, generated code will be written to the currently active editor</i></html>")
        checkboxDesc.foreground = JBUI.CurrentTheme.ContextHelp.FOREGROUND
        panel.add(checkboxDesc, gbc)

        // Spacer
        gbc.gridy = 4
        gbc.weighty = 1.0
        panel.add(Box.createVerticalGlue(), gbc)

        return panel
    }

    override fun doOKAction() {
        // Save settings
        settings.tfIdentifierPath = tfIdentifierPathField.text.trim()
        settings.autoWriteToEditor = autoWriteCheckBox.isSelected
        super.doOKAction()
    }
}
