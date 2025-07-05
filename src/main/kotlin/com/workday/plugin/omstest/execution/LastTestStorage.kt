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
    var environment: Environment? = null  // LOCAL or REMOTE

    // Local
    var runTabName: String? = null  // e.g. "UnionParsingTest", test class or test method name
    var targetName: String? = null  // e.g. -PtestClass=com.workday.extensible.externalfields.expressions.UnionParsingTest

    // Remote
    var host: String? = null  // e.g. "i-04a49a28050b6ddf5.workdaysuv.com"
    var fqTestName: String? = null  // fully qualified test name, e.g. "com.workday.extensible.externalfields.expressions.UnionParsingTest#testUnionParsing"
    var jmxParams: String? = null  // JMX parameters, e.g. "-PtestClass=com.workday.extensible.externalfields.expressions.UnionParsingTest -PtestMethod=testUnionParsing"

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