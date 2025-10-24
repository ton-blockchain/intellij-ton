package org.ton.intellij.tolk.inspection

import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo
import com.intellij.codeInspection.*
import com.intellij.execution.Executor
import com.intellij.execution.ProgramRunnerUtil
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.TestStateStorage
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ExecutionUtil
import com.intellij.execution.testframework.TestIconMapper
import com.intellij.openapi.editor.colors.CodeInsightColors
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Iconable
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.util.ui.UIUtil
import com.intellij.execution.testframework.sm.runner.states.TestStateInfo
import org.ton.intellij.tolk.ide.linemarker.TolkTestLineMarkerProvider
import org.ton.intellij.tolk.psi.TolkFunction
import org.ton.intellij.tolk.psi.TolkVisitor
import org.ton.intellij.tolk.psi.impl.isTestFunction
import javax.swing.Icon

@Suppress("UnstableApiUsage")
class TolkTestFailedLineInspection : TolkInspectionBase() {
    override fun buildTolkVisitor(
        holder: ProblemsHolder,
        session: LocalInspectionToolSession,
    ): TolkVisitor = object : TolkVisitor() {
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

            val quickFixes = mutableListOf<LocalQuickFix>()
            quickFixes.add(RunActionFix(function))

            val descriptor = InspectionManager.getInstance(holder.project).createProblemDescriptor(
                failedElement,
                "Test failed: ${state.errorMessage ?: "Unknown error"}",
                quickFixes.toTypedArray(),
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                holder.isOnTheFly,
                false
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

    private class RunActionFix(element: PsiElement) : LocalQuickFix, Iconable {
        private val executor: Executor = DefaultRunExecutor.getRunExecutorInstance()
        private val configuration: RunnerAndConfigurationSettings? = ConfigurationContext(element).configuration

        override fun getFamilyName(): String =
            UIUtil.removeMnemonic(executor.getStartActionText(ProgramRunnerUtil.shortenName(configuration?.name ?: "Test", 0)))

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val context = ConfigurationContext(descriptor.psiElement).configuration ?: return
            ExecutionUtil.runConfiguration(context, executor)
        }

        override fun generatePreview(project: Project, previewDescriptor: ProblemDescriptor): IntentionPreviewInfo =
            IntentionPreviewInfo.Html("Restarts this failed test")

        override fun availableInBatchMode(): Boolean = false

        override fun getIcon(flags: Int): Icon = executor.icon
    }
}
