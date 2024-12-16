package org.ton.intellij.tolk.inspection

import com.intellij.codeInsight.intention.FileModifier
import com.intellij.codeInspection.*
import com.intellij.codeInspection.util.InspectionMessage
import com.intellij.codeInspection.util.IntentionName
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElementVisitor
import org.ton.intellij.tolk.psi.TolkElement
import org.ton.intellij.tolk.psi.TolkVisitor
import org.ton.intellij.util.findExistingEditor

abstract class TolkAbstractApplicabilityBasedInspection<TElement : TolkElement>(
    val elementType: Class<TElement>,
) : TolkInspectionBase() {
    abstract val defaultFixText: String

    open val startFixInWriteAction = true

    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
        session: LocalInspectionToolSession,
    ): PsiElementVisitor = object : TolkVisitor() {
        override fun visitElement(o: TolkElement) {
            super.visitElement(o)

            if (!elementType.isInstance(o) || o.textLength == 0) {
                return
            }

            @Suppress("UNCHECKED_CAST")
            visitTargetElement(o as TElement, holder, isOnTheFly)
        }
    }

    protected fun visitTargetElement(element: TElement, holder: ProblemsHolder, isOnTheFly: Boolean) {
        if (!isApplicable(element)) {
            return
        }

        holder.registerProblemWithoutOfflineInformation(
            element,
            inspectionText(element),
            isOnTheFly,
            inspectionHighlightType(element),
            inspectionHighlightRangeInElement(element),
            LocalFix(this, fixText(element))
        )
    }

    open fun inspectionHighlightRangeInElement(element: TElement): TextRange? = null

    open fun inspectionHighlightType(element: TElement): ProblemHighlightType =
        ProblemHighlightType.GENERIC_ERROR_OR_WARNING

    @InspectionMessage
    abstract fun inspectionText(element: TElement): String

    @IntentionName
    open fun fixText(element: TElement) = defaultFixText

    abstract fun isApplicable(element: TElement): Boolean

    abstract fun applyTo(element: TElement, project: Project = element.project, editor: Editor? = null)

    private class LocalFix<TElement : TolkElement>(
        @FileModifier.SafeFieldForPreview val inspection: TolkAbstractApplicabilityBasedInspection<TElement>,
        @IntentionName val text: String,
    ) : LocalQuickFix {
        override fun startInWriteAction() = inspection.startFixInWriteAction

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            @Suppress("UNCHECKED_CAST")
            val element = descriptor.psiElement as TElement
            inspection.applyTo(element, project, element.findExistingEditor())
        }

        override fun getFamilyName() = inspection.defaultFixText

        override fun getName() = text
    }
}
