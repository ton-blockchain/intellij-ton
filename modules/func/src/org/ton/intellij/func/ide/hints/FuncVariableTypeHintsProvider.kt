package org.ton.intellij.func.ide.hints

import com.intellij.codeInsight.hints.declarative.*
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.ton.intellij.func.psi.*
import org.ton.intellij.func.type.ty.FuncTy
import org.ton.intellij.func.type.ty.FuncTyUnknown

class FuncVariableTypeHintsProvider : InlayHintsProvider {
    override fun createCollector(file: PsiFile, editor: Editor): InlayHintsCollector = Collector()

    private class Collector : SharedBypassCollector {
        override fun collectFromElement(element: PsiElement, sink: InlayTreeSink) {
            if (element is FuncBinExpression) {
                processVariableDeclaration(element, sink)
            }
        }

        private fun processVariableDeclaration(binExpression: FuncBinExpression, sink: InlayTreeSink) {
            val binaryOp = binExpression.binaryOp
            if (binaryOp.eq == null) return

            val rawLeft = binExpression.left
            if (rawLeft is FuncTensorExpression) {
                val inference = binExpression.inference ?: return
                for (expr in rawLeft.expressionList) {
                    val expr = expr as? FuncApplyExpression ?: return
                    if (expr.left !is FuncHoleTypeExpression) continue
                    if (expr.right?.text == "_") continue

                    val exprType = inference.getExprTy(expr.right ?: return)
                    if (exprType is FuncTyUnknown) return

                    val variableEnd = expr.textRange.endOffset
                    sink.showType(variableEnd, exprType)
                }
            }

            val left = rawLeft as? FuncApplyExpression ?: return
            if (left.left !is FuncHoleTypeExpression) return

            val variablesList = left.right
            if (variablesList is FuncTensorExpression) {
                val inference = binExpression.inference ?: return
                for (expr in variablesList.expressionList) {
                    if (expr.text == "_") continue

                    val exprType = inference.getExprTy(expr ?: return)
                    if (exprType is FuncTyUnknown) return

                    val variableEnd = expr.textRange.endOffset
                    sink.showType(variableEnd, exprType)
                }
                return
            }

            val variable = variablesList as? FuncReferenceExpression ?: return
            if (binExpression.right?.text == "_") return
            val inference = binExpression.inference ?: return

            val rightType = inference.getExprTy(binExpression.right ?: return)
            if (rightType is FuncTyUnknown) return

            val variableEnd = variable.textRange.endOffset
            sink.showType(variableEnd, rightType)
        }

        private fun InlayTreeSink.showType(
            variableEnd: Int,
            exprType: FuncTy,
        ) {
            this.addPresentation(
                InlineInlayPosition(variableEnd, true),
                listOf(),
                null,
                HintFormat.default.withColorKind(HintColorKind.Parameter)
            ) {
                text(": ")
                text(exprType.toString())
            }
        }
    }
}
