package org.ton.intellij.tolk.inspection

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import org.ton.intellij.tolk.psi.*
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

            override fun visitReferenceTypeExpression(o: TolkReferenceTypeExpression) {
                super.visitReferenceTypeExpression(o)
                val resolved = o.reference?.resolve() ?: return
                if ((resolved is TolkTypeDef && resolved.isDeprecated) || (resolved is TolkStruct && resolved.isDeprecated)) {
                    holder.registerProblem(
                        o,
                        "Deprecated type: ${resolved.name}",
                        ProblemHighlightType.LIKE_DEPRECATED
                    )
                }
            }

            override fun visitReferenceExpression(o: TolkReferenceExpression) {
                val resolved = o.reference?.resolve() ?: return
                when(resolved) {
                    is TolkFunction -> {
                        if (resolved.isDeprecated) {
                            holder.registerProblem(
                                o,
                                "Deprecated function reference: ${resolved.name}",
                                ProblemHighlightType.LIKE_DEPRECATED
                            )
                        }
                    }
                    is TolkTypeDef -> {
                        if (resolved.isDeprecated) {
                            holder.registerProblem(
                                o,
                                "Deprecated type alias reference: ${resolved.name}",
                                ProblemHighlightType.LIKE_DEPRECATED
                            )
                        }
                    }
                    is TolkStruct -> {
                        if (resolved.isDeprecated) {
                            holder.registerProblem(
                                o,
                                "Deprecated structure reference: ${resolved.name}",
                                ProblemHighlightType.LIKE_DEPRECATED
                            )
                        }
                    }
                    is TolkConstVar -> {
                        if (resolved.isDeprecated) {
                            holder.registerProblem(
                                o,
                                "Deprecated constant reference: ${resolved.name}",
                                ProblemHighlightType.LIKE_DEPRECATED
                            )
                        }
                    }
                    is TolkGlobalVar -> {
                        if (resolved.isDeprecated) {
                            holder.registerProblem(
                                o,
                                "Deprecated global variable reference: ${resolved.name}",
                                ProblemHighlightType.LIKE_DEPRECATED
                            )
                        }
                    }
                }
            }

            override fun visitFunction(o: TolkFunction) {
                if (o.isDeprecated) {
                    holder.registerProblem(
                        o.nameIdentifier ?: return,
                        "Deprecated function: ${o.name}",
                        ProblemHighlightType.LIKE_DEPRECATED
                    )
                }
            }

            override fun visitStruct(o: TolkStruct) {
                super.visitStruct(o)
                if (o.isDeprecated) {
                    holder.registerProblem(
                        o.nameIdentifier ?: return,
                        "Deprecated structure: ${o.name}",
                        ProblemHighlightType.LIKE_DEPRECATED
                    )
                }
            }

            override fun visitTypeDef(o: TolkTypeDef) {
                super.visitTypeDef(o)
                if (o.isDeprecated) {
                    holder.registerProblem(
                        o.nameIdentifier ?: return,
                        "Deprecated type alias: ${o.name}",
                        ProblemHighlightType.LIKE_DEPRECATED
                    )
                }
            }

            override fun visitConstVar(o: TolkConstVar) {
                super.visitConstVar(o)
                if (o.isDeprecated) {
                    holder.registerProblem(
                        o.nameIdentifier ?: return,
                        "Deprecated constant: ${o.name}",
                        ProblemHighlightType.LIKE_DEPRECATED
                    )
                }
            }

            override fun visitGlobalVar(o: TolkGlobalVar) {
                super.visitGlobalVar(o)
                if (o.isDeprecated) {
                    holder.registerProblem(
                        o.nameIdentifier ?: return,
                        "Deprecated global variable: ${o.name}",
                        ProblemHighlightType.LIKE_DEPRECATED
                    )
                }
            }
        }
    }
}
