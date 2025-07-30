package org.ton.intellij.tolk.codeInsight.hint

import com.intellij.codeInsight.hints.declarative.HintColorKind
import com.intellij.codeInsight.hints.declarative.HintFormat
import com.intellij.codeInsight.hints.declarative.HintMarginPadding
import com.intellij.codeInsight.hints.declarative.InlayHintsProvider
import com.intellij.codeInsight.hints.declarative.InlayTreeSink
import com.intellij.codeInsight.hints.declarative.InlineInlayPosition
import com.intellij.codeInsight.hints.declarative.SharedBypassCollector
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.ton.intellij.tolk.eval.TolkConstantExpressionEvaluator
import org.ton.intellij.tolk.eval.TolkIntValue
import org.ton.intellij.tolk.psi.TolkThrowStatement
import org.ton.intellij.util.exitcodes.generateShortExitCodeDocumentation

class TolkExitCodeHintsProvider : InlayHintsProvider {
    override fun createCollector(file: PsiFile, editor: Editor) = Collector()

    class Collector : SharedBypassCollector {
        override fun collectFromElement(element: PsiElement, sink: InlayTreeSink) {
            if (element !is TolkThrowStatement) return

            val code = element.expressionList.firstOrNull() ?: return
            val intCode = TolkConstantExpressionEvaluator.compute(element.project, code) ?: return
            if (intCode !is TolkIntValue) return

            val description = generateShortExitCodeDocumentation(intCode.value.intValueExact()) ?: return

            sink.addPresentation(
                InlineInlayPosition(code.textRange.endOffset, true),
                listOf(),
                null, HintFormat.default.withColorKind(HintColorKind.Parameter).withHorizontalMargin(HintMarginPadding.MarginAndSmallerPadding)
            ) {
                text(description)
            }
        }
    }
}
