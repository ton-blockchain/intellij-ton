package org.ton.intellij.tolk.inspection

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import org.ton.intellij.tolk.psi.TolkCallExpression
import org.ton.intellij.tolk.psi.TolkFunction
import org.ton.intellij.tolk.psi.TolkVisitor
import org.ton.intellij.tolk.psi.impl.functionSymbol
import org.ton.intellij.tolk.psi.impl.isDeprecated

class TolkDeprecationInspection : TolkInspectionBase() {
    override fun buildTolkVisitor(holder: ProblemsHolder, session: LocalInspectionToolSession): TolkVisitor {
        return object : TolkVisitor() {
            override fun visitCallExpression(o: TolkCallExpression) {
                super.visitCallExpression(o)
                val functionSymbol = o.functionSymbol ?: return
                if (functionSymbol.isDeprecated) {
                    holder.registerProblem(
                        o.expression,
                        "Deprecated function call",
                        ProblemHighlightType.LIKE_DEPRECATED
                    )
                }
            }

            override fun visitFunction(o: TolkFunction) {
                if (o.isDeprecated) {
                    val message = "Deprecated function: ${o.name}"
                    holder.registerProblem(
                        o.nameIdentifier ?: o,
                        message,
                        ProblemHighlightType.LIKE_DEPRECATED
                    )
                }
            }
        }
    }
}
