package org.ton.intellij.func.ide.hints

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
import org.ton.intellij.func.psi.FuncFunction
import org.ton.intellij.func.psi.impl.computeMethodId

class FuncMethodIdHintsProvider : InlayHintsProvider {
    override fun createCollector(file: PsiFile, editor: Editor) = Collector()

    class Collector : SharedBypassCollector {
        override fun collectFromElement(element: PsiElement, sink: InlayTreeSink) {
            if (element is FuncFunction && element.methodIdDefinition != null) {
                val (id, explicit) = element.computeMethodId()
                if (explicit) {
                    return
                }

                val anchor = element.methodIdDefinition ?: element.firstChild

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
