package org.ton.intellij.tact.annotator

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.tree.IElementType
import org.ton.intellij.tact.highlighting.TactColor
import org.ton.intellij.tact.psi.TactConstant
import org.ton.intellij.tact.psi.TactElement
import org.ton.intellij.tact.psi.TactElementTypes.IDENTIFIER
import org.ton.intellij.tact.psi.TactField
import org.ton.intellij.tact.psi.TactFieldExpression
import org.ton.intellij.tact.psi.TactFunction

class TactHighlightingAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (element !is LeafPsiElement) return
        val elementType = element.elementType

        val color = highlightLeaf(element, elementType, holder) ?: return

        holder.newSilentAnnotation(HighlightSeverity.INFORMATION).textAttributes(color.textAttributesKey).create()
    }

    private fun highlightLeaf(element: PsiElement, elementType: IElementType, holder: AnnotationHolder): TactColor? {
        val parent = element.parent as? TactElement ?: return null

        return when (elementType) {
            IDENTIFIER -> highlightIdentifier(element, parent, holder)
            else -> null
        }
    }

    private fun highlightIdentifier(element: PsiElement, parent: TactElement, holder: AnnotationHolder): TactColor? {
        if (element.text == "self") {
            return TactColor.SELF_PARAMETER
        }
        return colorFor(parent)
    }

    fun colorFor(element: TactElement) = when (element) {
        is TactFieldExpression -> {
            if (element.reference?.resolve() != null) {
                TactColor.FIELD
            } else {
                null
            }
        }
        is TactFunction -> TactColor.FUNCTION_DECLARATION
        is TactField -> TactColor.FIELD
        is TactConstant -> TactColor.CONSTANT
        else -> null
    }

}
