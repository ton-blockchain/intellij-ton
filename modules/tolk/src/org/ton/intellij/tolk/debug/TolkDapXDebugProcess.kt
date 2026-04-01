package org.ton.intellij.tolk.debug

import com.intellij.execution.ExecutionResult
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.ui.ExecutionConsole
import com.intellij.platform.dap.DapDebugSession
import com.intellij.platform.dap.DapStartRequest
import com.intellij.platform.dap.DebugAdapterDescriptor
import com.intellij.platform.dap.xdebugger.DapXDebugProcess
import com.intellij.xdebugger.XDebugSession
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
    startRequestArguments
) {
    override fun createConsole(): ExecutionConsole {
        return backingExecutionResult.executionConsole ?: super.createConsole()
    }
}
