package org.ton.intellij.tolk.highlighting

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import org.ton.intellij.tolk.doc.psi.*
import org.ton.intellij.tolk.ide.colors.TolkColor
import org.ton.intellij.tolk.psi.TolkElementTypes
import org.ton.intellij.util.ancestorStrict
import org.ton.intellij.util.descendantOfTypeStrict

class TolkDocAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (holder.isBatchMode) return
        val color = when (element.elementType) {
            TolkDocElementTypes.DOC_TEXT -> when (element.parent) {
                is TolkDocCodeFence -> TolkColor.DOC_CODE
                is TolkDocCodeFenceStartEnd, is TolkDocCodeFenceLang -> TolkColor.DOC_CODE
                is TolkDocCodeSpan -> if (element.ancestorStrict<TolkDocLink>() == null) {
                    TolkColor.DOC_CODE
                } else {
                    null
                }

//                is TolkDocLinkLabel -> TolkColor.IDENTIFIER
                is TolkDocCodeBlock -> TolkColor.DOC_CODE
                else -> null
            }

            TolkElementTypes.LBRACK, TolkElementTypes.RBRACK -> when (element.parent) {
                is TolkDocLinkLabel, is TolkDocLinkText -> TolkColor.BRACKETS
                else -> null
            }

            TolkElementTypes.LPAREN, TolkElementTypes.RPAREN -> when (element.parent) {
                is TolkDocInlineLink -> TolkColor.PARENTHESES
                else -> null
            }

            else -> null
        } ?: when {
            element is TolkDocLink && element.descendantOfTypeStrict<TolkDocGap>() == null -> TolkColor.DOC_LINK
            else -> null
        } ?: return
        holder.newSilentAnnotation(HighlightSeverity.INFORMATION).textAttributes(color.textAttributesKey).create()
    }
}
