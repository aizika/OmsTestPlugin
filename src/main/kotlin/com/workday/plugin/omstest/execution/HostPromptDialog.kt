package com.workday.plugin.omstest.execution

import com.intellij.openapi.ui.DialogWrapper
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

/**
 * Dialog for prompting the user to enter a remote host.
 * This dialog is used when running remote OMS tests.
 *
 * @author alexander.aizikivsky
 * @since Jun-2025
 */
class HostPromptDialog : DialogWrapper(true) {
    private val panel = JPanel(BorderLayout())
    private val hostTextField = JTextField()

    init {
        title = "Enter Remote Host"
        hostTextField.columns = 30  // or: hostTextField.preferredSize = Dimension(400, 24)
        init()
        panel.add(JLabel("Host:"), BorderLayout.WEST)
        panel.add(hostTextField, BorderLayout.CENTER)

        // Set initial host from LastTestStorage
        hostTextField.text = LastTestStorage.host ?: ""

        // Enable OK only if the host is non-empty
        validateInput()
        hostTextField.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) = validateInput()
            override fun removeUpdate(e: DocumentEvent) = validateInput()
            override fun changedUpdate(e: DocumentEvent) = validateInput()
        })
    }

    override fun createCenterPanel(): JComponent = panel

    fun getHost(): String = normalizeHost(hostTextField.text.trim())

    fun normalizeHost(input: String): String {
        val regex = Regex("""i-[a-f0-9]+""")
        val id = regex.find(input)?.value ?: return input
        return "$id.workdaysuv.com"
    }

    override fun doOKAction() {
        // Save host when OK is pressed
        LastTestStorage.host = getHost()
        super.doOKAction()
    }

    private fun validateInput() {
        isOKActionEnabled = hostTextField.text.trim().isNotEmpty()
    }
}