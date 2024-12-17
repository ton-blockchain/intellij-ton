package org.ton.intellij.func.highlighting

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import org.ton.intellij.func.doc.psi.*
import org.ton.intellij.func.psi.FuncElementTypes
import org.ton.intellij.util.ancestorStrict
import org.ton.intellij.util.descendantOfTypeStrict

class FuncDocAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (holder.isBatchMode) return
        val color = when (element.elementType) {
            FuncDocElementTypes.DOC_TEXT -> when (element.parent) {
                is FuncDocCodeFence -> FuncColor.DOC_CODE
                is FuncDocCodeFenceStartEnd, is FuncDocCodeFenceLang -> FuncColor.DOC_CODE
                is FuncDocCodeSpan -> if (element.ancestorStrict<FuncDocLink>() == null) {
                    FuncColor.DOC_CODE
                } else {
                    null
                }

//                is FuncDocLinkLabel -> FuncColor.IDENTIFIER
                is FuncDocCodeBlock -> FuncColor.DOC_CODE
                else -> null
            }

            FuncElementTypes.LBRACK, FuncElementTypes.RBRACK -> when (element.parent) {
                is FuncDocLinkLabel, is FuncDocLinkText -> FuncColor.BRACKETS
                else -> null
            }

            FuncElementTypes.LPAREN, FuncElementTypes.RPAREN -> when (element.parent) {
                is FuncDocInlineLink -> FuncColor.PARENTHESES
                else -> null
            }

            else -> null
        } ?: when {
            element is FuncDocLink && element.descendantOfTypeStrict<FuncDocGap>() == null -> FuncColor.DOC_LINK
            else -> null
        } ?: return
        holder.newSilentAnnotation(HighlightSeverity.INFORMATION).textAttributes(color.textAttributesKey).create()
    }
}
