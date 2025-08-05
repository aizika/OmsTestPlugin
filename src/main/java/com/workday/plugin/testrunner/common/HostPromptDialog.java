package com.workday.plugin.testrunner.common;

import java.awt.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.jetbrains.annotations.Nullable;

import com.intellij.openapi.ui.DialogWrapper;

/**
 *  Dialog to prompt the user for a remote host.
 * This dialog is used to enter the host for running tests on a remote server.
 * It validates the input and normalizes it to a specific format.
 *
 * @author alexander.aizikivsky
 * @since Jun-2025
 */
public class HostPromptDialog
    extends DialogWrapper {

    private final JPanel panel = new JPanel(new BorderLayout());
    private final JTextField hostTextField = new JTextField();

    public HostPromptDialog() {
        super(true); // use the current window as parent
        setTitle("Enter Remote Host");
        hostTextField.setColumns(30);

        // Set initial value from LastTestStorage
        hostTextField.setText(LastTestStorage.getHost() != null ? LastTestStorage.getHost() : "");

        panel.add(new JLabel("Host:"), BorderLayout.WEST);
        panel.add(hostTextField, BorderLayout.CENTER);

        init(); // must be called after panel setup

        validateInput();
        hostTextField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                validateInput();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                validateInput();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                validateInput();
            }
        });
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return panel;
    }

    public String getHost() {
        return normalizeHost(hostTextField.getText().trim());
    }

    private String normalizeHost(String input) {
        Pattern pattern = Pattern.compile("i-[a-f0-9]+");
        Matcher matcher = pattern.matcher(input);
        if (matcher.find()) {
            String id = matcher.group();
            return id + ".workdaysuv.com";
        }
        return input;
    }

    @Override
    protected void doOKAction() {
        LastTestStorage.setHost(getHost());
        super.doOKAction();
    }

    private void validateInput() {
        setOKActionEnabled(!hostTextField.getText().trim().isEmpty());
    }
}