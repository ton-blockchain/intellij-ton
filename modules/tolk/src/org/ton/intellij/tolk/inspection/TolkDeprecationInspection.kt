package org.ton.intellij.tolk.inspection

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import org.ton.intellij.tolk.psi.TolkAnnotationHolder
import org.ton.intellij.tolk.psi.TolkElement
import org.ton.intellij.tolk.psi.TolkReferenceElement
import org.ton.intellij.tolk.psi.TolkVisitor

class TolkDeprecationInspection : TolkInspectionBase() {
    override fun buildTolkVisitor(holder: ProblemsHolder, session: LocalInspectionToolSession): TolkVisitor {
        return object : TolkVisitor() {
            override fun visitElement(ref: TolkElement) {
                if (ref !is TolkReferenceElement) return

                val original = ref.reference?.resolve() ?: return
                val identifier = ref.referenceNameElement ?: return

                if (original is TolkAnnotationHolder && original.annotations.hasDeprecatedAnnotation()) {
                    holder.registerProblem(identifier, "Deprecated", ProblemHighlightType.LIKE_DEPRECATED)
                }
            }
        }
    }
}
