package com.workday.plugin.omstest.run

import com.intellij.execution.process.ProcessHandler
import java.io.OutputStream

class RemoteTestProcessHandler : ProcessHandler() {
    override fun destroyProcessImpl() {
        notifyProcessTerminated(0)
    }

    override fun detachProcessImpl() {
        notifyProcessDetached()
    }

    override fun detachIsDefault() = false

    override fun getProcessInput(): OutputStream? = null
    fun finish(exitCode: Int = 0) {
        notifyProcessTerminated(exitCode)
    }
}