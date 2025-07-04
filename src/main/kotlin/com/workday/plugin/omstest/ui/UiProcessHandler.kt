package com.workday.plugin.omstest.ui

import com.intellij.execution.process.ProcessHandler
import java.io.OutputStream

/**
 * Custom process handler for JUnit tests that allows pushing output and finishing the process.
 * This is used to simulate a process that can be controlled programmatically, such as in a test runner.
 *
 * @author alexander.aizikivsky
 * @since Jun-2025
 */
class UiProcessHandler : ProcessHandler() {

    /**
     *  notifyProcessTerminated is protected, so we need this method to terminate the process.
     */
    fun finish() {
        notifyProcessTerminated(0)
    }

    // the methods below are required to implement ProcessHandler
    override fun destroyProcessImpl() {
        notifyProcessTerminated(0)
    }

    override fun detachProcessImpl() {
        notifyProcessDetached()
    }

    override fun detachIsDefault(): Boolean = false

    override fun getProcessInput(): OutputStream? = null

}