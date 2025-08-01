package org.ton.intellij.tlb.ide.hints

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
import org.ton.intellij.tlb.computeTag
import org.ton.intellij.tlb.psi.TlbConstructor

class TlbImplicitConstructorTagHintsProvider : InlayHintsProvider {
    override fun createCollector(file: PsiFile, editor: Editor) = Collector()

    class Collector : SharedBypassCollector {
        override fun collectFromElement(element: PsiElement, sink: InlayTreeSink) {
            if (element is TlbConstructor && element.constructorTag == null) {
                val computedTag = element.computeTag()
                if (computedTag != null) {
                    sink.addPresentation(
                        InlineInlayPosition(element.firstChild.textRange.endOffset, true),
                        listOf(),
                        null, HintFormat.default.withColorKind(HintColorKind.Parameter).withHorizontalMargin(HintMarginPadding.OnlyPadding)
                    ) {
                        text(computedTag.toString())
                    }
                }
            }
        }
    }
}
