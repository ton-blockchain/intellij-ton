package org.ton.intellij.func.highlighting

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.ton.intellij.func.psi.*

class FuncAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        when (element) {
            is FuncTypeParameter -> {
                highlight(element.identifier, holder, FuncSyntaxHighlightingColors.TYPE_PARAMETER.textAttributesKey)
                return
            }

            is FuncTypeIdentifier -> {
                highlight(element.identifier, holder, FuncSyntaxHighlightingColors.TYPE_PARAMETER.textAttributesKey)
                return
            }

            is FuncGlobalVar -> {
                highlight(
                    element.identifier ?: return,
                    holder,
                    FuncSyntaxHighlightingColors.GLOBAL_VARIABLE.textAttributesKey
                )
                return
            }

            is FuncConstVar -> {
                highlight(
                    element.identifier ?: return,
                    holder,
                    FuncSyntaxHighlightingColors.CONSTANT.textAttributesKey
                )
                return
            }

            is FuncFunctionParameter -> {
                highlight(
                    element.identifier ?: return,
                    holder,
                    FuncSyntaxHighlightingColors.PARAMETER.textAttributesKey
                )
                return
            }

            is FuncReferenceExpression -> {
                if (element.node.treeParent.elementType == FuncElementTypes.CALL_EXPRESSION) {
                    highlight(
                        element.identifier,
                        holder,
                        FuncSyntaxHighlightingColors.FUNCTION_STATIC.textAttributesKey
                    )
                } else {
                    val resolved = element.reference?.resolve() ?: element
                    var color: TextAttributesKey? = null
                    PsiTreeUtil.treeWalkUp(resolved, null) { scope, _ ->
                        color = when (scope) {
                            is FuncBlockStatement -> FuncSyntaxHighlightingColors.LOCAL_VARIABLE.textAttributesKey
                            is FuncConstVar -> FuncSyntaxHighlightingColors.CONSTANT.textAttributesKey
                            is FuncGlobalVar -> FuncSyntaxHighlightingColors.GLOBAL_VARIABLE.textAttributesKey
                            is FuncFunctionParameter -> FuncSyntaxHighlightingColors.PARAMETER.textAttributesKey
                            else -> null
                        }
                        color == null
                    }
                    color?.let {
                        highlight(
                            element.identifier,
                            holder,
                            it
                        )
                    }
                }
                return
            }
        }
        val parent = element.parent
        when (parent) {
            is FuncFunction -> {
                if (element == parent.nameIdentifier) {
                    highlight(element, holder, FuncSyntaxHighlightingColors.FUNCTION_DECLARATION.textAttributesKey)
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
