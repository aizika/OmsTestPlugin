package com.workday.plugin.testrunner.common;

import java.awt.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;

/**
 * Dialog to prompt the user for a remote host.
 * Keeps history of recent hosts and validates input as EC2 instance IDs.
 *
 * @author alexander.aizikivsky
 * @since Jun-2025
 */
public class HostPromptDialog extends DialogWrapper {

    private final JPanel panel = new JPanel(new BorderLayout());
    private final ComboBox<String> hostCombo = new ComboBox<>();

    public HostPromptDialog() {
        super(true);
        setTitle("Enter Remote Host");

        hostCombo.setEditable(true);
        ((JTextField) hostCombo.getEditor().getEditorComponent()).setColumns(30);

        // prime with history + last used (if any)
        List<String> history = LastTestStorage.getRecentHosts();
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
        for (String h : history) model.addElement(h);
        String last = LastTestStorage.getHost();
        if (last != null && !last.isBlank() && model.getIndexOf(last) == -1) {
            model.insertElementAt(last, 0);
        }
        hostCombo.setModel(model);
        if (model.getSize() > 0) hostCombo.setSelectedIndex(0);

        panel.add(new JLabel("Host:"), BorderLayout.WEST);
        panel.add(hostCombo, BorderLayout.CENTER);

        init();

        // validation wiring for editable combo
        JTextField editor = (JTextField) hostCombo.getEditor().getEditorComponent();
        validateInput(editor.getText());
        editor.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { validateInput(editor.getText()); }
            @Override public void removeUpdate(DocumentEvent e) { validateInput(editor.getText()); }
            @Override public void changedUpdate(DocumentEvent e) { validateInput(editor.getText()); }
        });
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return panel;
    }

    public String getHost() {
        // keep your normalization to EC2 id + suffix
        Pattern pattern = Pattern.compile("i-[a-f0-9]{17}");
        Matcher matcher = pattern.matcher(getRawHost());
        if (matcher.find()) {
            String id = matcher.group();
            return id + ".workdaysuv.com";
        }
        return "";
    }

    private @NotNull String getRawHost() {
        Object sel = hostCombo.getEditor().getItem();
        return sel == null ? "" : sel.toString().trim();
    }

    @Override
    protected void doOKAction() {
        String normalized = getHost();
        if (!normalized.isBlank()) {
            LastTestStorage.setHost(normalized);          // preserve existing behavior
            LastTestStorage.addRecentHost(normalized);    // NEW: push into history
        }
        super.doOKAction();
    }

    private void validateInput(String text) {
        Pattern pattern = Pattern.compile("i-[a-f0-9]{17}");
        Matcher matcher = pattern.matcher(text.trim());
        setOKActionEnabled(matcher.find());
    }
}
