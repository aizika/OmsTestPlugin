import com.intellij.execution.process.*
import com.intellij.openapi.util.Key
import java.io.OutputStream

class LocalProcessHandler(private val wrapped: OSProcessHandler) : ProcessHandler() {
    @Volatile private var terminated = false
    @Volatile private var terminating = false

    init {
        // Forward output and lifecycle events
        wrapped.addProcessListener(object : ProcessListener {
            override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
                notifyTextAvailable(event.text, outputType)
            }

            override fun processWillTerminate(event: ProcessEvent, willBeDestroyed: Boolean) {
                terminating = true
            }

            override fun processTerminated(event: ProcessEvent) {
                terminated = true
                notifyProcessTerminated(event.exitCode)
            }
        })
    }

    override fun startNotify() {
        notifyStartSafe() // <— this is our workaround
        wrapped.startNotify()
    }

    override fun destroyProcessImpl() {
        terminating = true
        wrapped.destroyProcess()
    }

    override fun detachProcessImpl() {
        terminating = true
        wrapped.detachProcess()
    }

    override fun detachIsDefault(): Boolean = false
    override fun isProcessTerminated(): Boolean = terminated
    override fun isProcessTerminating(): Boolean = terminating
    override fun getProcessInput(): OutputStream? = wrapped.processInput

    // ✳️ Workaround to access protected notifyStartNotified()
    private fun notifyStartSafe() {
        super.startNotify() // will internally call notifyStartNotified
    }
}