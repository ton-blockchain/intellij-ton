package org.ton.intellij.tolk.ide.test.configuration

import com.intellij.execution.ExecutionException
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.execution.runners.ExecutionUtil
import com.intellij.execution.runners.ProgramRunner
import com.intellij.execution.testframework.AbstractTestProxy
import com.intellij.execution.testframework.Filter
import com.intellij.execution.testframework.TestConsoleProperties
import com.intellij.execution.testframework.actions.AbstractRerunFailedTestsAction
import com.intellij.execution.testframework.sm.runner.SMTestProxy
import com.intellij.execution.ui.ConsoleView
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ExecutionDataKeys
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.ton.intellij.acton.cli.ActonCommand
import org.ton.intellij.acton.runconfig.ActonCommandConfiguration
import org.ton.intellij.acton.runconfig.ActonCommandRunState
import org.ton.intellij.acton.runconfig.TestCommandOverride

class TolkRerunFailedTestsAction(consoleView: ConsoleView, consoleProperties: TestConsoleProperties) :
    AbstractRerunFailedTestsAction(consoleView) {

    init {
        init(consoleProperties)
    }

    override fun actionPerformed(e: AnActionEvent) {
        val environment = e.getData(ExecutionDataKeys.EXECUTION_ENVIRONMENT) ?: return
        val configuration = myConsoleProperties.configuration as? ActonCommandConfiguration ?: return
        val selection = buildRerunSelection(configuration.testTarget, getFailedTests(configuration.project))
        rerunWithSelection(environment, configuration, selection)
    }

    override fun getFilter(project: Project, searchScope: GlobalSearchScope): Filter<*> =
        super.getFilter(project, searchScope).and(
            object : Filter<AbstractTestProxy>() {
                override fun shouldAccept(test: AbstractTestProxy): Boolean =
                    test.isLeaf && (test as? SMTestProxy)?.isSuite() != true
            },
        )

    private fun rerunWithSelection(
        environment: ExecutionEnvironment,
        configuration: ActonCommandConfiguration,
        selection: RerunSelection,
    ) {
        val runner = ProgramRunner.getRunner(environment.executor.id, configuration) ?: return
        val builder = ExecutionEnvironmentBuilder(environment)
            .runProfile(configuration)
            .runner(runner)

        try {
            val rerunEnvironment = builder.build()
            rerunEnvironment.putUserData(
                ActonCommandRunState.TEST_COMMAND_OVERRIDE_KEY,
                TestCommandOverride(
                    mode = ActonCommand.Test.TestMode.FUNCTION,
                    target = selection.target,
                    functionName = selection.filterPattern,
                ),
            )
            runner.execute(rerunEnvironment)
        } catch (e: ExecutionException) {
            ExecutionUtil.handleExecutionError(environment, e)
        }
    }

    companion object {
        internal fun buildRerunSelection(
            originalTestTarget: String,
            failedTests: Collection<AbstractTestProxy>,
        ): RerunSelection {
            val testsFromLocations = failedTests
                .asSequence()
                .mapNotNull { failedTest ->
                    val location = TolkTestLocator.parseLocationUrl(failedTest.locationUrl) ?: return@mapNotNull null
                    FailedTest(
                        target = location.filePath,
                        name = location.functionName,
                    )
                }
                .distinct()
                .toList()

            val tests = testsFromLocations.ifEmpty {
                failedTests
                    .asSequence()
                    .filter { failedTest -> failedTest.isLeaf && (failedTest as? SMTestProxy)?.isSuite() != true }
                    .map { failedTest ->
                        FailedTest(
                            target = null,
                            name = failedTest.name,
                        )
                    }
                    .distinct()
                    .toList()
            }

            require(tests.isNotEmpty()) { "Rerun selection requires at least one failed test" }

            val narrowedTarget = tests
                .mapNotNull { it.target }
                .distinct()
                .singleOrNull()
                ?.takeIf { tests.all { test -> test.target != null } }
                ?: originalTestTarget

            val escapedNames = tests.map { failedTest -> escapeRustRegexLiteral(failedTest.name) }
            val filterPattern = if (escapedNames.size == 1) {
                "^${escapedNames.single()}$"
            } else {
                escapedNames.joinToString(prefix = "^(?:", separator = "|", postfix = ")$")
            }

            return RerunSelection(narrowedTarget, filterPattern)
        }

        internal data class RerunSelection(val target: String, val filterPattern: String)

        private data class FailedTest(val target: String?, val name: String)

        private fun escapeRustRegexLiteral(value: String): String {
            val builder = StringBuilder(value.length)
            for (char in value) {
                if (char in RUST_REGEX_META_CHARACTERS) {
                    builder.append('\\')
                }
                builder.append(char)
            }
            return builder.toString()
        }

        private val RUST_REGEX_META_CHARACTERS =
            setOf('\\', '.', '+', '*', '?', '(', ')', '|', '[', ']', '{', '}', '^', '$')
    }
}
