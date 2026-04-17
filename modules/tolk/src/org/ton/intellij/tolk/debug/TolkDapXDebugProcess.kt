package org.ton.intellij.tolk.debug

import com.intellij.execution.ExecutionResult
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.ui.ExecutionConsole
import com.intellij.openapi.diagnostic.logger
import com.intellij.platform.dap.DapDebugSession
import com.intellij.platform.dap.DapStartRequest
import com.intellij.platform.dap.DebugAdapterDescriptor
import com.intellij.platform.dap.xdebugger.DapXDebugProcess
import com.intellij.xdebugger.XDebugSession
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider
import kotlinx.coroutines.CoroutineScope

internal class TolkDapXDebugProcess(
    session: XDebugSession,
    dapDebugSession: DapDebugSession,
    xDebugProcessScope: CoroutineScope,
    globalScope: CoroutineScope,
    debugAdapterDescriptor: DebugAdapterDescriptor<*>,
    executionEnvironment: ExecutionEnvironment,
    private val backingExecutionResult: ExecutionResult,
    startRequestType: DapStartRequest,
    startRequestArguments: Map<String, Any?>,
) : DapXDebugProcess(
    session,
    dapDebugSession,
    xDebugProcessScope,
    globalScope,
    debugAdapterDescriptor,
    executionEnvironment,
    backingExecutionResult,
    startRequestType,
    startRequestArguments,
) {
    init {
        runCatching {
            val field = DapXDebugProcess::class.java.getDeclaredField("presentationFactory")
            field.isAccessible = true
            field.set(this, TolkDapXDebuggerPresentationFactory)
        }.onFailure {
            LOG.warn("Failed to install custom Tolk DAP presentation factory", it)
        }
    }

    override fun createConsole(): ExecutionConsole = backingExecutionResult.executionConsole ?: super.createConsole()

    override fun getEditorsProvider(): XDebuggerEditorsProvider = TolkDebuggerEditorsProvider

    companion object {
        private val LOG = logger<TolkDapXDebugProcess>()
    }
}
