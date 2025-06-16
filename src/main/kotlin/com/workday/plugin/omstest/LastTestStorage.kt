package com.workday.plugin.omstest


/**
 * Storage for the last executed test command and its label.
 * Used to enable re-running the last test without needing to reconfigure the command.
 *
 * @author alexander.aizikivsky
 * @since Jun-2025
 */
object LastTestStorage {
    var lastCommand: List<String>? = null
    var lastLabel: String? = null
}