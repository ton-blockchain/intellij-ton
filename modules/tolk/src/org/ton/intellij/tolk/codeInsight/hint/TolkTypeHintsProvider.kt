package org.ton.intellij.tolk.codeInsight.hint

import com.intellij.codeInsight.hints.declarative.InlayTreeSink
import com.intellij.codeInsight.hints.declarative.InlineInlayPosition
import com.intellij.psi.PsiElement
import org.ton.intellij.tolk.psi.TolkCatchParameter
import org.ton.intellij.tolk.psi.TolkConstVar
import org.ton.intellij.tolk.psi.TolkFunction
import org.ton.intellij.tolk.psi.TolkVar
import org.ton.intellij.tolk.type.TolkFunctionTy
import org.ton.intellij.tolk.type.TolkTy
import org.ton.intellij.tolk.type.printTolkType

class TolkTypeHintsProvider : AbstractTolkInlayHintProvider() {
    override fun collectFromElement(
        element: PsiElement,
        sink: InlayTreeSink
    ) {
        sink.whenOptionEnabled("hint.type.var") {
            collectFromVar(element, sink)
            collectFromCatch(element, sink)
        }
        sink.whenOptionEnabled("hint.type.const") {
            collectFromConst(element, sink)
        }
        sink.whenOptionEnabled("hint.type.fun") {
            collectFromFunction(element, sink)
        }
    }

    private fun collectFromVar(element: PsiElement, sink: InlayTreeSink) {
        if (element !is TolkVar) return
        if (!element.isValid) return
        if (element.typeExpression != null) return
        val name = element.name
        if (name.isNullOrEmpty() || name == "_") return
        val type = element.type ?: return
        if (type == TolkTy.Unknown) return

        sink.addPresentation(
            position = InlineInlayPosition(element.textRange.endOffset, true),
//            hintFormat = HintFormat.default
            hasBackground = true,
        ) {
            text(": ")
            printTolkType(type)
        }
    }

    private fun collectFromCatch(element: PsiElement, sink: InlayTreeSink) {
        if (element !is TolkCatchParameter) return
        if (element.name == "_") return
        val type = element.type ?: return
        sink.addPresentation(
            position = InlineInlayPosition(element.textRange.endOffset, true),
//            hintFormat = HintFormat.default,
            hasBackground = true,
        ) {
            text(": ")
            printTolkType(type)
        }
    }

    private fun collectFromConst(element: PsiElement, sink: InlayTreeSink) {
        if (element !is TolkConstVar) return
        if (element.typeExpression != null) return
        val identifier = element.identifier ?: return
        val type = element.type ?: return
        sink.addPresentation(
            position = InlineInlayPosition(identifier.textRange.endOffset, true),
//            hintFormat = HintFormat.default,
            hasBackground = true,
        ) {
            text(": ")
            printTolkType(type)
        }
    }

    private fun collectFromFunction(element: PsiElement, sink: InlayTreeSink) {
        if (element !is TolkFunction) return
        if (element.returnType != null) return
        element.functionBody?.blockStatement ?: return
        val parameters = element.parameterList ?: return
        val returnType = (element.type as? TolkFunctionTy)?.returnType ?: return
        when (returnType) {
            TolkTy.Void,
            TolkTy.Unknown -> return
            else -> {}
        }
        sink.addPresentation(
            position = InlineInlayPosition(parameters.textRange.endOffset, true),
//            hintFormat = HintFormat.default,
            hasBackground = true,
        ) {
            text(": ")
            printTolkType(returnType)
        }
    }
}
