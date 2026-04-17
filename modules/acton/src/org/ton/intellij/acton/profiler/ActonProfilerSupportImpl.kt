package org.ton.intellij.acton.profiler

import com.intellij.execution.Executor
import com.intellij.execution.process.KillableColoredProcessHandler
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.profiler.actions.ImportProfilerResultAction
import com.intellij.profiler.api.ProfilerDumpDescriptor
import com.intellij.profiler.api.ProfilerDumpManager
import com.intellij.profiler.clion.ProfilerExecutor
import org.ton.intellij.acton.runconfig.ActonCommandConfiguration

class ActonProfilerSupportImpl : ActonProfilerSupport {
    override fun createTestSession(
        configuration: ActonCommandConfiguration,
        executor: Executor,
    ): ActonProfilerSession? {
        if (executor.id != ProfilerExecutor.EXECUTOR_ID) return null

        val dumpManager = ProfilerDumpManager.getInstance(configuration.project)
        val dumpName = configuration.name.takeIf { it.isNotBlank() } ?: "Acton Test"
        val profilerDump = dumpManager.createDump(dumpName, CPU_PROFILE_PARSER_PROVIDER)
        return Session(configuration, profilerDump)
    }

    private class Session(
        private val configuration: ActonCommandConfiguration,
        private val profilerDump: ProfilerDumpDescriptor,
    ) : ActonProfilerSession {
        override val additionalArguments: List<String> = listOf(
            "--profile-format",
            "collapsed",
            "--cpuprofile",
            profilerDump.file.absolutePath,
        )

        override fun attachToProcess(handler: KillableColoredProcessHandler) {
            handler.addProcessListener(object : ProcessAdapter() {
                override fun processTerminated(event: ProcessEvent) {
                    val dumpFile = profilerDump.file
                    if (!dumpFile.exists() || dumpFile.length() == 0L) {
                        LOG.warn(
                            "Acton CPU profile was not produced for '${configuration.name}' at ${dumpFile.absolutePath}",
                        )
                        profilerDump.remove()
                        return
                    }

                    LOG.info("Importing Acton CPU profile from ${dumpFile.absolutePath}")
                    ApplicationManager.getApplication().invokeLater {
                        if (configuration.project.isDisposed) {
                            profilerDump.remove()
                            return@invokeLater
                        }
                        ImportProfilerResultAction.Companion.importProfilerDump(
                            configuration.project,
                            profilerDump,
                            null,
                            null,
                        )
                    }
                }
            })
        }
    }

    companion object {
        private val LOG = logger<ActonProfilerSupportImpl>()
        private val CPU_PROFILE_PARSER_PROVIDER = ActonCollapsedProfileDumpParserProvider()
    }
}
