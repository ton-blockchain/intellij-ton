package org.ton.intellij.tolk.codeInsight.hint

import com.intellij.codeInsight.hints.declarative.HintFormat
import com.intellij.codeInsight.hints.declarative.InlayTreeSink
import com.intellij.codeInsight.hints.declarative.InlineInlayPosition
import com.intellij.psi.PsiElement
import com.intellij.psi.util.endOffset
import org.ton.intellij.tolk.psi.*
import org.ton.intellij.tolk.type.printTolkType
import java.util.*

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
        if (element.typeExpression != null) return
        val name = element.name
        if (name.isNullOrEmpty() || name == "_") return
        val type = element.type ?: return

        sink.addPresentation(
            position = InlineInlayPosition(element.endOffset, true),
            hintFormat = HintFormat.default
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
            position = InlineInlayPosition(element.endOffset, true),
            hintFormat = HintFormat.default
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
            position = InlineInlayPosition(identifier.endOffset, true),
            hintFormat = HintFormat.default
        ) {
            text(": ")
            printTolkType(type)
        }
    }

    private fun collectFromFunction(element: PsiElement, sink: InlayTreeSink) {
        if (element !is TolkFunction) return
        if (element.typeExpression != null) return
        val block = element.functionBody?.blockStatement ?: return
        val parameters = element.parameterList ?: return

        val returnStatements = LinkedList<TolkReturnStatement>()
        object : TolkRecursiveElementWalkingVisitor() {
            override fun elementFinished(element: PsiElement) {
                if (element is TolkReturnStatement) {
                    returnStatements.add(element)
                }
            }

            override fun visitElement(element: PsiElement) {
                if (element is TolkExpressionStatement) return
                super.visitElement(element)
            }
        }.visitElement(block)

        val returnType = returnStatements.asSequence()
            .mapNotNull { it.expression?.type }
            .firstOrNull() ?: return

        sink.addPresentation(
            position = InlineInlayPosition(parameters.endOffset, true),
            hintFormat = HintFormat.default
        ) {
            text(": ")
            printTolkType(returnType)
        }
    }
}