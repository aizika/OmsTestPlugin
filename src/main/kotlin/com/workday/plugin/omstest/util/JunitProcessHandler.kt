package com.workday.plugin.omstest.util

import com.intellij.execution.process.ProcessHandler
import com.intellij.openapi.util.Key
import java.io.OutputStream

class JunitProcessHandler : ProcessHandler() {
    @Volatile private var terminated = false
    @Volatile private var terminating = false

    override fun destroyProcessImpl() {
        terminating = true
        terminated = true
        notifyProcessTerminated(0)
    }

    override fun detachProcessImpl() {
        terminating = true
        terminated = true
        notifyProcessDetached()
    }

    override fun detachIsDefault(): Boolean = false

    override fun isProcessTerminated(): Boolean = terminated
    override fun isProcessTerminating(): Boolean = terminating

    override fun getProcessInput(): OutputStream? = null

    fun start() {
        startNotify()
    }

    fun finish() {
        destroyProcessImpl()
    }

    fun pushOutput(text: String, outputType: Key<*>) {
        notifyTextAvailable(text, outputType)
    }
}