package org.ton.intellij.func.highlighting

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.psi.PsiElement
import org.ton.intellij.func.psi.FuncElementTypes
import org.ton.intellij.func.psi.FuncFunction
import org.ton.intellij.func.psi.FuncReferenceExpression
import org.ton.intellij.func.psi.FuncTypeParameter

class FuncAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        val parent = element.parent
        when (parent) {
            is FuncFunction -> {
                if (element == parent.nameIdentifier) {
                    highlight(element, holder, FuncSyntaxHighlightingColors.FUNCTION_DECLARATION.textAttributesKey)
                }
            }

            else -> when (element) {
                is FuncTypeParameter -> {
                    highlight(element.identifier, holder, FuncSyntaxHighlightingColors.TYPE_PARAMETER.textAttributesKey)
                }


                is FuncReferenceExpression -> {
                    if (element.node.treeParent.elementType == FuncElementTypes.FUNCTION_CALL_EXPRESSION) {
                        highlight(
                            element.identifier,
                            holder,
                            FuncSyntaxHighlightingColors.FUNCTION_STATIC.textAttributesKey
                        )
                    } else {
                        highlight(
                            element.identifier,
                            holder,
                            FuncSyntaxHighlightingColors.LOCAL_VARIABLE.textAttributesKey
                        )
                    }
                }
            }
        }
    }

    private fun highlight(element: PsiElement, holder: AnnotationHolder, key: TextAttributesKey) {
        holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
            .range(element.textRange)
            .textAttributes(key)
            .create()
    }
}
