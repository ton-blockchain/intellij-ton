package org.ton.intellij.tolk.ide

import com.intellij.execution.ExecutionListener
import com.intellij.execution.ExecutionManager
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.testframework.sm.runner.ui.SMTRunnerConsoleView
import com.intellij.execution.ui.RunContentManager
import com.intellij.execution.ui.ConsoleView
import com.intellij.openapi.project.Project
import com.intellij.util.messages.MessageBusConnection
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.util.Key
import com.intellij.psi.search.GlobalSearchScope

/**
 * This class is a workaround for registering filters for tests.
 * For some reason [TolkConsoleFilterProvider] doesn't work in tests :(
 *
 * So we explicitly listen for events when a process starts running via Run/Debug and explicitly attach filters.
 */
class TolkAttachTestConsoleFiltersAtStartup : ProjectActivity {
    override suspend fun execute(project: Project) {
        val conn: MessageBusConnection = project.messageBus.connect()

        conn.subscribe(ExecutionManager.EXECUTION_TOPIC, object : ExecutionListener {
            override fun processStarted(executorId: String, env: ExecutionEnvironment, handler: ProcessHandler) {
                handler.addProcessListener(object : ProcessAdapter() {
                    override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
                        val descriptor = RunContentManager.getInstance(project).allDescriptors.lastOrNull() ?: return
                        val console = descriptor.executionConsole as? ConsoleView ?: return
                        addFilters(project, console)
                    }
                })
            }
        })
    }

    private fun addFilters(project: Project, console: ConsoleView) {
        if (console !is SMTRunnerConsoleView) return
        console.addMessageFilter(TolkConsoleFilter(project, GlobalSearchScope.allScope(project)))
    }
}
