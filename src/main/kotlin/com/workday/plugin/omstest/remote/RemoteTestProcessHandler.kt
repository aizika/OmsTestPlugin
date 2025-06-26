import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.openapi.util.Key
import java.io.OutputStream

class RemoteTestProcessHandler : ProcessHandler() {
    override fun destroyProcessImpl() {
        notifyProcessTerminated(0)
    }

    override fun detachProcessImpl() {
        notifyProcessDetached()
    }

    override fun detachIsDefault(): Boolean = false

    override fun isProcessTerminated(): Boolean = isTerminated

    override fun isProcessTerminating(): Boolean = isTerminated
    override fun getProcessInput(): OutputStream? {
        TODO("Not yet implemented")
    }

    private var isTerminated = false

    fun finish(exitCode: Int) {
        isTerminated = true
        notifyProcessTerminated(exitCode)
    }

    fun printToConsole(text: String) {
        notifyTextAvailable(text, ProcessOutputTypes.STDOUT)
    }

    override fun startNotify() {
        super.startNotify()
    }
}