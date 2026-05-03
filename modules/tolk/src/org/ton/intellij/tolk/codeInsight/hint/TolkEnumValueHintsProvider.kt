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
import org.ton.intellij.tolk.eval.TolkEnumValueEvaluator
import org.ton.intellij.tolk.psi.TolkEnumMember

class TolkEnumValueHintsProvider : InlayHintsProvider {
    override fun createCollector(file: PsiFile, editor: Editor) = Collector()

    class Collector : SharedBypassCollector {
        override fun collectFromElement(element: PsiElement, sink: InlayTreeSink) {
            if (element !is TolkEnumMember) return

            val value = TolkEnumValueEvaluator.compute(element) ?: return
            val expression = element.expression
            if (expression != null && expression.text == value.toString()) return

            val anchor = expression ?: element.identifier

            sink.addPresentation(
                InlineInlayPosition(anchor.textRange.endOffset, true),
                listOf(),
                null,
                HintFormat.default.withColorKind(
                    HintColorKind.Parameter,
                ).withHorizontalMargin(HintMarginPadding.MarginAndSmallerPadding),
            ) {
                text(" = ")
                text(value.toString())
            }
        }
    }
}
