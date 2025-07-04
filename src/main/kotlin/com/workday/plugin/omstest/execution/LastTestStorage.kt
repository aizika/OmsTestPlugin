package com.workday.plugin.omstest.execution

/**
 * Storage for the last executed test command and its label.
 * Used to enable re-running the last test without needing to reconfigure the command.
 *
 * @author alexander.aizikivsky
 * @since Jun-2025
 */
object LastTestStorage {
    enum class Environment {
        LOCAL, REMOTE
    }


    // Either local or remote
    var environment: Environment? = null

    // Local
    var runTabName: String? = null
    var targetName: String? = null

    // Remote
    var host: String? = null
    var fqTestName: String? = null
    var jmxParams: String? = null

    fun setLocal(label: String, targetName: String) {
        this.runTabName = label
        this.targetName = targetName
        this.environment = Environment.LOCAL

        // Clear remote-related fields
        this.fqTestName = null
        this.jmxParams = null
    }

    fun setRemote(host: String, fqTestName: String, params: String, runTabName: String) {
        this.runTabName = runTabName
        this.environment = Environment.REMOTE
        this.host = host
        this.fqTestName = fqTestName
        this.jmxParams = params

        // Clear local-related fields
        this.targetName = null

    }
}