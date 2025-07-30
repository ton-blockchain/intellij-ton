package org.ton.intellij.fift.ide.hints

import com.intellij.codeInsight.hints.declarative.*
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.ton.intellij.fift.psi.FiftAsmExpression
import org.ton.intellij.fift.psi.isNotInstruction
import org.ton.intellij.util.asm.findInstruction
import org.ton.intellij.util.asm.instructionPresentation

class FiftAssemblyInfoHintsProvider : InlayHintsProvider {
    override fun createCollector(file: PsiFile, editor: Editor) = Collector()

    class Collector : SharedBypassCollector {
        override fun collectFromElement(element: PsiElement, sink: InlayTreeSink) {
            if (element !is FiftAsmExpression) return

            val instr = element.tvmInstruction
            if (instr.isNotInstruction()) return
            val name = instr.text
            if (name == "INLINECALLDICT") return

            val arguments = element.asmArgumentList?.asmPrimitiveList ?: emptyList()

            val info = findInstruction(name, arguments)

            val presentation = instructionPresentation(info?.doc?.gas, info?.doc?.stack, "{gas}")

            sink.addPresentation(
                InlineInlayPosition(element.textRange.endOffset, true),
                listOf(),
                null,
                HintFormat.default
                    .withColorKind(HintColorKind.TextWithoutBackground)
                    .withHorizontalMargin(HintMarginPadding.MarginAndSmallerPadding)
                    .withFontSize(HintFontSize.AsInEditor)
            ) {
                text(presentation)
            }
        }
    }
}
