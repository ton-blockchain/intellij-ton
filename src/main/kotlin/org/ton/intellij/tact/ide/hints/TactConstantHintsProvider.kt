package org.ton.intellij.tact.ide.hints

import com.intellij.codeInsight.hints.declarative.*
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.refactoring.suggested.endOffset
import org.ton.intellij.tact.eval.evaluate
import org.ton.intellij.tact.psi.TactConstant
import org.ton.intellij.tact.psi.TactIntegerExpression

class TactConstantHintsProvider : InlayHintsProvider {
    override fun createCollector(file: PsiFile, editor: Editor): InlayHintsCollector =
        Collector()

    private class Collector : SharedBypassCollector {
        override fun collectFromElement(element: PsiElement, sink: InlayTreeSink) {
            if (element !is TactConstant) return
            processConstant(element, sink)
        }

        fun processConstant(constant: TactConstant, sink: InlayTreeSink) {
            val expression = constant.expression ?: return
            if (expression is TactIntegerExpression) {
                return
            }
            val value = constant.expression?.evaluate() ?: return
            sink.addPresentation(InlineInlayPosition(constant.identifier.endOffset, true), hasBackground = true) {
                val text = buildString {
                    append("= ")
                    append(value.toString())
                }
                text(text)
            }
        }
    }
}
