package org.ton.intellij.acton.profiler

import com.intellij.execution.Executor
import com.intellij.execution.process.KillableColoredProcessHandler
import com.intellij.openapi.extensions.ExtensionPointName
import org.ton.intellij.acton.runconfig.ActonCommandConfiguration

interface ActonProfilerSupport {
    fun createTestSession(configuration: ActonCommandConfiguration, executor: Executor): ActonProfilerSession?

    companion object {
        val EP_NAME = ExtensionPointName.create<ActonProfilerSupport>("org.ton.intellij.acton.profilerSupport")
    }
}

interface ActonProfilerSession {
    val additionalArguments: List<String>

    fun attachToProcess(handler: KillableColoredProcessHandler)
}
