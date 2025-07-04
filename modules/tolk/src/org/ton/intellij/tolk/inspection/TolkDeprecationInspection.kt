package org.ton.intellij.tolk.inspection

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import org.ton.intellij.tolk.psi.*

class TolkDeprecationInspection : TolkInspectionBase() {
    override fun buildTolkVisitor(holder: ProblemsHolder, session: LocalInspectionToolSession): TolkVisitor {
        return object : TolkVisitor() {
            override fun visitElement(ref: TolkElement) {
                if (ref !is TolkReferenceElement) return

                val original = ref.reference?.resolve() ?: return
                val identifier = ref.referenceNameElement ?: return
                val name = ref.referenceName ?: return

                if (original is TolkAnnotationHolder && original.annotations.hasDeprecatedAnnotation()) {
                    val deprecatedAnnotation = original.annotations.deprecatedAnnotation()
                    val text =
                        (deprecatedAnnotation?.argumentList?.argumentList?.firstOrNull()?.expression as? TolkLiteralExpression)?.stringLiteral?.rawString?.text
                            ?: ""
                    val formattedText = if (text.isNotBlank()) {
                        " $text."
                    } else {
                        ""
                    }
                    holder.registerProblem(
                        identifier,
                        "$name is deprecated.$formattedText",
                        ProblemHighlightType.LIKE_DEPRECATED
                    )
                }
            }
        }
    }
}
