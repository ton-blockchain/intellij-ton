package org.ton.intellij.tolk.highlighting.checkers

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.HighlightSeverity
import org.ton.intellij.tolk.psi.TolkBinExpression
import org.ton.intellij.tolk.psi.TolkConstVar
import org.ton.intellij.tolk.psi.TolkParameter
import org.ton.intellij.tolk.psi.TolkVar
import org.ton.intellij.tolk.psi.TolkVarExpression
import org.ton.intellij.tolk.psi.impl.isSetAssignment
import org.ton.intellij.tolk.psi.unwrapParentheses
import org.ton.intellij.util.parentOfType

class TolkCommonProblemsChecker(holder: AnnotationHolder) : TolkCheckerBase(holder) {
    override fun visitBinExpression(bin: TolkBinExpression) {
        if (bin.binaryOp.eq != null || bin.isSetAssignment) {
            checkAssignmentToImmutable(bin)
        }
    }

    private fun checkAssignmentToImmutable(bin: TolkBinExpression) {
        val left = bin.left.unwrapParentheses() ?: return
        val resolved = left.reference?.resolve()

        if (resolved is TolkParameter) {
            // parameter can always be reassigned
            return
        }

        if (resolved is TolkVar) {
            val expression = resolved.parentOfType<TolkVarExpression>() ?: return
            if (expression.varKeyword != null) {
                // ok, can reassign `var some`
                return
            }

            holder.newAnnotation(HighlightSeverity.ERROR, "Cannot reassign immutable variable '${resolved.name}'")
                .create()
        }

        if (resolved is TolkConstVar) {
            holder.newAnnotation(HighlightSeverity.ERROR, "Cannot reassign constant '${resolved.name}'")
                .create()
        }
    }
}
