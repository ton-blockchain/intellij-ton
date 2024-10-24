package org.ton.intellij.tolk.ide.hints

import com.intellij.codeInsight.hints.declarative.*
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile
import com.intellij.refactoring.suggested.endOffset
import org.ton.intellij.tolk.eval.TolkConstantExpressionEvaluator
import org.ton.intellij.tolk.eval.TolkIntValue
import org.ton.intellij.tolk.psi.TolkFile
import org.ton.intellij.tolk.psi.TolkLiteralExpression
import org.ton.intellij.util.exception.ConstantEvaluationOverflowException

class TolkConstantHintsProvider : InlayHintsProvider {
    override fun createCollector(file: PsiFile, editor: Editor): InlayHintsCollector {
        return Collector()
    }

    private class Collector : OwnBypassCollector {
        override fun collectHintsForFile(file: PsiFile, sink: InlayTreeSink) {
            if (file !is TolkFile) return

            file.constVars.forEach { const ->
                val constIdentifier = const.identifier ?: return
                val expression = const.expression ?: return@forEach
                if (expression is TolkLiteralExpression && !expression.showHint()) {
                    return@forEach
                }
                val value = const.expression?.let {
                    try {
                        TolkConstantExpressionEvaluator.compute(file.project, it, true)
                    } catch (e: ConstantEvaluationOverflowException) {
                        return@forEach
                    }
                } ?: return@forEach
                sink.addPresentation(InlineInlayPosition(constIdentifier.endOffset, true), hasBackground = true) {
                    val text = buildString {
                        append("= ")
                        if (value is TolkIntValue &&
                            expression is TolkLiteralExpression &&
                            expression.stringLiteral != null
                        ) {
                            append("0x")
                            val hex = value.value.toString(16)
                            if (hex.length > 8) {
                                append(hex.substring(0, 4))
                                append("...")
                                append(hex.substring(hex.length - 4))
                            } else {
                                append(hex)
                            }
                        } else {
                            append(value.toString())
                        }
                    }
                    text(text)
                }
            }
        }

        private fun TolkLiteralExpression.showHint(): Boolean {
            if (integerLiteral != null) return false
            if (trueKeyword != null) return false
            if (falseKeyword != null) return false
            return true
        }
    }
}
