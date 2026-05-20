package org.jetbrains.plugins.designer.ui.dialogs

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import org.jetbrains.plugins.designer.services.BackendConfigService
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.geom.RoundRectangle2D
import javax.swing.*

class ChangeBackendDialog(private val project: Project) : DialogWrapper(project) {

    private lateinit var filePathField: JTextField
    private lateinit var changeNoField: JTextField
    private lateinit var fixRadio: JRadioButton
    private lateinit var devRadio: JRadioButton
    private val configService = BackendConfigService.getInstance(project)

    init {
        title = "Change Değiştir"
        init()
    }

    override fun createCenterPanel(): JComponent {
        val mainPanel = object : JPanel() {
            init {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                border = JBUI.Borders.empty(20, 24, 20, 24)
                background = JBColor(Color(255, 255, 255), Color(43, 45, 48))
            }

            override fun paintComponent(g: Graphics) {
                val g2d = g as Graphics2D
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                g2d.color = background
                g2d.fillRoundRect(0, 0, width, height, 16, 16)
                super.paintComponent(g)
            }
        }

        val headerLabel = JBLabel("Change Değiştir").apply {
            font = Font("SF Pro Display", Font.BOLD, 18)
            foreground = JBColor(Color(30, 41, 59), Color(241, 245, 249))
            alignmentX = Component.LEFT_ALIGNMENT
        }

        mainPanel.add(headerLabel)
        mainPanel.add(Box.createVerticalStrut(14))

        val radioPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            background = JBColor(Color(255, 255, 255), Color(43, 45, 48))
            alignmentX = Component.LEFT_ALIGNMENT
            isOpaque = false
        }

        fixRadio = JRadioButton("Fix").apply {
            font = Font("SF Pro Display", Font.PLAIN, 13)
            foreground = JBColor(Color(55, 65, 81), Color(209, 213, 219))
            background = JBColor(Color(255, 255, 255), Color(43, 45, 48))
            isOpaque = false
            isSelected = true
        }

        devRadio = JRadioButton("Dev").apply {
            font = Font("SF Pro Display", Font.PLAIN, 13)
            foreground = JBColor(Color(55, 65, 81), Color(209, 213, 219))
            background = JBColor(Color(255, 255, 255), Color(43, 45, 48))
            isOpaque = false
        }

        val buttonGroup = ButtonGroup()
        buttonGroup.add(fixRadio)
        buttonGroup.add(devRadio)

        radioPanel.add(fixRadio)
        radioPanel.add(Box.createHorizontalStrut(16))
        radioPanel.add(devRadio)
        radioPanel.add(Box.createHorizontalGlue())

        mainPanel.add(radioPanel)
        mainPanel.add(Box.createVerticalStrut(14))

        val filePathLabel = JBLabel("Properties Dosya").apply {
            font = Font("SF Pro Display", Font.PLAIN, 12)
            foreground = JBColor(Color(71, 85, 105), Color(203, 213, 225))
            alignmentX = Component.LEFT_ALIGNMENT
        }
        mainPanel.add(filePathLabel)
        mainPanel.add(Box.createVerticalStrut(6))

        val filePathPanel = createRoundedInputPanel().apply {
            alignmentX = Component.LEFT_ALIGNMENT

            val fileIcon = JLabel("📁").apply {
                font = font.deriveFont(16f)
            }
            add(fileIcon, BorderLayout.WEST)

            filePathField = object : JTextField() {
                init {
                    text = configService.getPropertiesFilePath()
                    font = Font("SF Pro Display", Font.PLAIN, 13)
                    background = JBColor(Color(249, 250, 251), Color(55, 58, 64))
                    foreground = JBColor(Color(55, 65, 81), Color(209, 213, 219))
                    border = JBUI.Borders.empty()
                    isOpaque = false
                    isEditable = false
                    cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)

                    addMouseListener(object : MouseAdapter() {
                        override fun mouseClicked(e: MouseEvent) {
                            val fileChooser = JFileChooser().apply {
                                fileSelectionMode = JFileChooser.FILES_ONLY
                                if (text.isNotBlank()) {
                                    currentDirectory = java.io.File(text).parentFile
                                }
                                fileFilter = object : javax.swing.filechooser.FileFilter() {
                                    override fun accept(f: java.io.File) = f.isDirectory || f.name.endsWith(".properties")
                                    override fun getDescription() = "Properties Files (*.properties)"
                                }
                            }
                            if (fileChooser.showOpenDialog(this@apply) == JFileChooser.APPROVE_OPTION) {
                                text = fileChooser.selectedFile.absolutePath
                                configService.setPropertiesFilePath(text)
                                updateChangeNumberFromFile()
                            }
                        }
                    })
                }

                override fun paintComponent(g: Graphics) {
                    val g2d = g as Graphics2D
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                    g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
                    super.paintComponent(g)
                }
            }
            add(filePathField, BorderLayout.CENTER)
        }
        mainPanel.add(filePathPanel)

        mainPanel.add(Box.createVerticalStrut(14))

        val changeNoLabel = JBLabel("Change Numarası").apply {
            font = Font("SF Pro Display", Font.PLAIN, 12)
            foreground = JBColor(Color(71, 85, 105), Color(203, 213, 225))
            alignmentX = Component.LEFT_ALIGNMENT
        }
        mainPanel.add(changeNoLabel)
        mainPanel.add(Box.createVerticalStrut(6))

        val changeNoPanel = createRoundedInputPanel().apply {
            alignmentX = Component.LEFT_ALIGNMENT

            val changeIcon = JLabel("🔢").apply {
                font = font.deriveFont(16f)
            }
            add(changeIcon, BorderLayout.WEST)

            changeNoField = object : JTextField() {
                init {
                    font = Font("SF Pro Display", Font.PLAIN, 13)
                    background = JBColor(Color(249, 250, 251), Color(55, 58, 64))
                    foreground = JBColor(Color(55, 65, 81), Color(209, 213, 219))
                    border = JBUI.Borders.empty()
                    isOpaque = false
                }

                override fun paintComponent(g: Graphics) {
                    val g2d = g as Graphics2D
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                    g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
                    super.paintComponent(g)
                }
            }
            add(changeNoField, BorderLayout.CENTER)
        }
        mainPanel.add(changeNoPanel)

        updateChangeNumberFromFile()

        return mainPanel
    }

    private fun createRoundedInputPanel(): JPanel {
        return object : JPanel(BorderLayout(12, 0)) {
            init {
                background = JBColor(Color(249, 250, 251), Color(55, 58, 64))
                border = JBUI.Borders.empty(10, 14)
                preferredSize = Dimension(420, 42)
                isOpaque = false
            }

            override fun paintComponent(g: Graphics) {
                val g2d = g as Graphics2D
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

                g2d.color = background
                g2d.fill(RoundRectangle2D.Float(0f, 0f, width.toFloat(), height.toFloat(), 12f, 12f))

                g2d.color = JBColor(Color(203, 213, 225), Color(71, 85, 105))
                g2d.setStroke(BasicStroke(1f))
                g2d.draw(RoundRectangle2D.Float(0.5f, 0.5f, width - 1f, height - 1f, 12f, 12f))

                super.paintComponent(g)
            }
        }
    }

    private fun updateChangeNumberFromFile() {
        val currentChange = configService.getCurrentChangeNumber()
        if (currentChange != null) {
            changeNoField.text = currentChange
        }
        val prefix = configService.getCurrentChangePrefix()
        if (prefix == "Dev") {
            devRadio.isSelected = true
        } else {
            fixRadio.isSelected = true
        }
    }

    override fun doOKAction() {
        val filePath = filePathField.text
        val changeNo = changeNoField.text

        if (filePath.isBlank()) {
            JOptionPane.showMessageDialog(
                contentPanel,
                "Lütfen properties dosya yolunu seçin",
                "Eksik Bilgi",
                JOptionPane.WARNING_MESSAGE
            )
            return
        }

        if (changeNo.isBlank()) {
            JOptionPane.showMessageDialog(
                contentPanel,
                "Lütfen change numarasını girin",
                "Eksik Bilgi",
                JOptionPane.WARNING_MESSAGE
            )
            return
        }

        try {
            val file = java.io.File(filePath)
            if (!file.exists()) {
                JOptionPane.showMessageDialog(
                    contentPanel,
                    "Dosya bulunamadı: $filePath",
                    "Hata",
                    JOptionPane.ERROR_MESSAGE
                )
                return
            }

            val cleanChangeNo = changeNo.replace(Regex("[^0-9]"), "")

            if (cleanChangeNo.isEmpty()) {
                JOptionPane.showMessageDialog(
                    contentPanel,
                    "Lütfen geçerli bir change numarası girin (örn: 144252)",
                    "Geçersiz Giriş",
                    JOptionPane.WARNING_MESSAGE
                )
                return
            }

            val prefix = if (fixRadio.isSelected) "Fix" else "Dev"

            val content = file.readText()
            val updatedContent = content.replace(Regex("(?:Dev|Fix)-\\d+"), "$prefix-$cleanChangeNo")

            val changeNumber = cleanChangeNo.toIntOrNull()
            val appTestReplacement = if (changeNumber != null && changeNumber % 2 == 0) {
                "apptest2"
            } else {
                "apptest"
            }

            val finalContent = updatedContent.replace(Regex("https://apptest2?\\."), "https://$appTestReplacement.")

            file.writeText(finalContent)

            configService.setPropertiesFilePath(filePath)

            JOptionPane.showMessageDialog(
                contentPanel,
                "✅ Backend başarıyla güncellendi!\n\n" +
                        "Change No: $prefix-$cleanChangeNo\n" +
                        "Environment: $appTestReplacement",
                "İşlem Başarılı",
                JOptionPane.INFORMATION_MESSAGE
            )

            super.doOKAction()
        } catch (e: Exception) {
            JOptionPane.showMessageDialog(
                contentPanel,
                "Dosya güncellenirken hata oluştu:\n${e.message}",
                "Hata",
                JOptionPane.ERROR_MESSAGE
            )
        }
    }

    override fun createActions(): Array<Action> {
        return arrayOf(okAction, cancelAction)
    }

    init {
        okAction.putValue(Action.NAME, "Uygula")
        cancelAction.putValue(Action.NAME, "İptal")
    }
}
