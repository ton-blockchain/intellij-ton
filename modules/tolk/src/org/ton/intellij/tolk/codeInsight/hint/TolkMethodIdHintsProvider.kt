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
import org.ton.intellij.tolk.psi.TolkFunction
import org.ton.intellij.tolk.psi.impl.computeMethodId
import org.ton.intellij.tolk.psi.impl.getKeyword
import org.ton.intellij.tolk.psi.impl.isGetMethod
import org.ton.intellij.tolk.psi.impl.isTestFunction

class TolkMethodIdHintsProvider : InlayHintsProvider {
    override fun createCollector(file: PsiFile, editor: Editor) = Collector()

    class Collector : SharedBypassCollector {
        override fun collectFromElement(element: PsiElement, sink: InlayTreeSink) {
            if (element is TolkFunction && element.isGetMethod && !element.isTestFunction()) {
                val (id, explicit) = element.computeMethodId()
                if (explicit) {
                    return
                }

                val anchor = element.getKeyword?.psi ?: element.firstChild

                sink.addPresentation(
                    InlineInlayPosition(anchor.textRange.endOffset, true),
                    listOf(),
                    null, HintFormat.default.withColorKind(HintColorKind.Parameter).withHorizontalMargin(HintMarginPadding.OnlyPadding)
                ) {
                    text("(")
                    text(id)
                    text(")")
                }
            }
        }
    }
}
