package org.ton.intellij.tolk.debug

import com.intellij.execution.ExecutionException
import com.intellij.execution.ExecutionResult
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.execution.ui.RunContentManager

internal object TolkDebugFailureRunContent {
    fun showIfTerminatedBeforeDap(
        environment: ExecutionEnvironment,
        executionResult: ExecutionResult,
        error: Throwable,
        title: String
    ): RunContentDescriptor? {
        if (error !is ExecutionException) return null
        val processHandler = executionResult.processHandler ?: return null
        if (!processHandler.isProcessTerminated) return null

        val console = executionResult.executionConsole ?: return null
        val descriptor = RunContentDescriptor(console, processHandler, console.component, title)
        RunContentManager.getInstance(environment.project).showRunContent(environment.executor, descriptor)
        return descriptor
    }
}
