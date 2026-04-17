package org.ton.intellij.tolk.inspection

import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo
import com.intellij.codeInspection.*
import com.intellij.execution.Executor
import com.intellij.execution.ProgramRunnerUtil
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.TestStateStorage
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.ExecutionUtil
import com.intellij.execution.testframework.TestIconMapper
import com.intellij.execution.testframework.sm.runner.states.TestStateInfo
import com.intellij.openapi.editor.colors.CodeInsightColors
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Iconable
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.util.ui.UIUtil
import com.intellij.xml.util.XmlStringUtil.escapeString
import org.ton.intellij.acton.runconfig.ComparisonFailure
import org.ton.intellij.acton.runconfig.actonTestFailureState
import org.ton.intellij.tolk.ide.linemarker.TolkTestLineMarkerProvider
import org.ton.intellij.tolk.ide.test.configuration.TolkTestLocator
import org.ton.intellij.tolk.psi.TolkFunction
import org.ton.intellij.tolk.psi.TolkVisitor
import org.ton.intellij.tolk.psi.impl.isTestFunction
import javax.swing.Icon

@Suppress("UnstableApiUsage")
class TolkTestFailedLineInspection : TolkInspectionBase() {
    override fun buildTolkVisitor(holder: ProblemsHolder, session: LocalInspectionToolSession): TolkVisitor =
        object : TolkVisitor() {
            private val testFailureState = holder.project.actonTestFailureState

            override fun visitFunction(function: TolkFunction) {
                if (!function.isTestFunction()) return

                val state = TolkTestLineMarkerProvider.getTestState(function)
                val magnitude = state?.let { TestIconMapper.getMagnitude(it.magnitude) } ?: return

                if (magnitude != TestStateInfo.Magnitude.ERROR_INDEX &&
                    magnitude != TestStateInfo.Magnitude.FAILED_INDEX
                ) {
                    return
                }

                val failedElement = findFailedElement(function, state)
                val locationUrl = TolkTestLocator.getTestUrl(function)
                val comparisonFailure = testFailureState.getComparisonFailure(locationUrl)

                val descriptor = InspectionManager.getInstance(holder.project).createProblemDescriptor(
                    failedElement,
                    buildProblemMessage(state, comparisonFailure),
                    arrayOf(DebugActionFix(function)),
                    ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                    holder.isOnTheFly,
                    false,
                )
                descriptor.setTextAttributes(CodeInsightColors.RUNTIME_ERROR)
                holder.registerProblem(descriptor)
            }

            private fun findFailedElement(function: TolkFunction, state: TestStateStorage.Record): PsiElement {
                val stacktrace = state.topStacktraceLine ?: return function

                val pattern = Regex("([^:]+):(\\d+):(\\d+)")
                val match = pattern.find(stacktrace) ?: return function.nameIdentifier ?: function

                val filePath = match.groupValues[1]
                val lineNumber = match.groupValues[2].toIntOrNull() ?: return function.nameIdentifier ?: function
                val lineColumn = match.groupValues[3].toIntOrNull() ?: 0

                val functionFile = function.containingFile.virtualFile
                if (!filePath.endsWith(functionFile.name)) {
                    return function.nameIdentifier ?: function
                }

                val document = PsiDocumentManager.getInstance(function.project).getDocument(function.containingFile)
                    ?: return function.nameIdentifier ?: function

                if (lineNumber - 1 >= document.lineCount) {
                    return function.nameIdentifier ?: function
                }

                val lineStartOffset = document.getLineStartOffset(lineNumber - 1)
                val element = function.containingFile.findElementAt(lineStartOffset + lineColumn)

                return element ?: function.nameIdentifier ?: function
            }
        }

    companion object {
        private val actualExpectedPattern = Regex("""(?s)\bActual:\s*(.*?)\n\s*Expected:\s*(.*)\z""")

        internal fun buildProblemMessage(
            state: TestStateStorage.Record,
            comparisonFailure: ComparisonFailure?,
        ): String {
            val message = summaryMessage(state.errorMessage)
            val details =
                comparisonFailure?.let { formatActualExpected(it.actual, it.expected) }
                    ?: extractActualExpectedDetails(state.errorMessage, state.topStacktraceLine)

            return if (details == null) {
                "<html>${escapeString("Test failed: $message")}</html>"
            } else {
                "<html>${escapeString("Test failed: $message")}<br><pre>${escapeString(details)}</pre></html>"
            }
        }

        private fun summaryMessage(errorMessage: String?): String = errorMessage
            ?.lineSequence()
            ?.firstOrNull { it.isNotBlank() }
            ?.trim()
            ?: "Unknown error"

        internal fun extractActualExpectedDetails(output: String?, stacktraceLine: String?): String? {
            if (output.isNullOrBlank()) return null

            val sanitized = sanitizeFailureOutput(output, stacktraceLine)
            val match = actualExpectedPattern.find(sanitized) ?: return null
            val actual = match.groupValues[1].trimEnd()
            val expected = match.groupValues[2].trimEnd()
            if (actual.isBlank() || expected.isBlank()) return null

            return formatActualExpected(actual, expected)
        }

        private fun sanitizeFailureOutput(output: String, stacktraceLine: String?): String {
            var text = output.replace("\r\n", "\n").replace('\r', '\n')
            if (!stacktraceLine.isNullOrBlank()) {
                text = text.replace(stacktraceLine, "")
            }
            text = text.replace(Regex("""\n?\s*at\s*\z"""), "")
            return text.trim()
        }

        private fun formatActualExpected(actual: String, expected: String): String {
            val actualLine =
                if ('\n' in actual) {
                    "Actual:\n$actual"
                } else {
                    "Actual:   $actual"
                }
            val expectedLine =
                if ('\n' in expected) {
                    "Expected:\n$expected"
                } else {
                    "Expected: $expected"
                }
            return "$actualLine\n$expectedLine"
        }
    }

    private class DebugActionFix(element: PsiElement) :
        LocalQuickFix,
        Iconable {
        private val executor: Executor = DefaultDebugExecutor.getDebugExecutorInstance()
        private val configuration: RunnerAndConfigurationSettings? = ConfigurationContext(element).configuration

        override fun getFamilyName(): String = UIUtil.removeMnemonic(
            executor.getStartActionText(
                ProgramRunnerUtil.shortenName(
                    configuration?.name ?: "Test",
                    0,
                ),
            ),
        )

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val context = ConfigurationContext(descriptor.psiElement).configuration ?: return
            ExecutionUtil.runConfiguration(context, executor)
        }

        override fun generatePreview(project: Project, previewDescriptor: ProblemDescriptor): IntentionPreviewInfo =
            IntentionPreviewInfo.Html("Starts a debug session for this failed test")

        override fun availableInBatchMode(): Boolean = false

        override fun getIcon(flags: Int): Icon = executor.icon
    }
}
