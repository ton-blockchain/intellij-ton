package org.ton.intellij.tolk.debug

import com.intellij.execution.ExecutionException
import com.intellij.execution.ExecutionResult
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.execution.ui.RunContentManager

internal object TolkDebugFailureRunContent {
    fun showIfTerminatedBeforeDap(
        environment: ExecutionEnvironment,
        executionResult: ExecutionResult,
        error: Throwable,
        title: String
    ): Boolean {
        if (error !is ExecutionException) return false
        val processHandler = executionResult.processHandler ?: return false
        if (!processHandler.isProcessTerminated) return false

        val console = executionResult.executionConsole ?: return false
        val descriptor = RunContentDescriptor(console, processHandler, console.component, title)
        RunContentManager.getInstance(environment.project)
            .showRunContent(DefaultRunExecutor.getRunExecutorInstance(), descriptor)
        return true
    }
}
