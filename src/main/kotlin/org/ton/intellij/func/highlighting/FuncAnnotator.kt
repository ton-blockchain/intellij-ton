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
                highlight(element.identifier, holder, FuncColor.TYPE_PARAMETER.textAttributesKey)
                return
            }

            is FuncTypeIdentifier -> {
                highlight(element.identifier, holder, FuncColor.TYPE_PARAMETER.textAttributesKey)
                return
            }

            is FuncGlobalVar -> {
                highlight(
                    element.identifier ?: return,
                    holder,
                    FuncColor.GLOBAL_VARIABLE.textAttributesKey
                )
                return
            }

            is FuncConstVar -> {
                highlight(
                    element.identifier ?: return,
                    holder,
                    FuncColor.CONSTANT.textAttributesKey
                )
                return
            }

            is FuncFunctionParameter -> {
                highlight(
                    element.identifier ?: return,
                    holder,
                    FuncColor.PARAMETER.textAttributesKey
                )
                return
            }

            is FuncReferenceExpression -> {
                when (element.node.treeParent.elementType) {
                    FuncElementTypes.CALL_EXPRESSION -> {
                        highlight(
                            element.identifier,
                            holder,
                            FuncColor.FUNCTION_STATIC.textAttributesKey
                        )
                    }

                    FuncElementTypes.METHOD_CALL -> {
                        highlight(
                            element.identifier,
                            holder,
                            FuncColor.FUNCTION_CALL.textAttributesKey
                        )
                    }

                    else -> {
                        val resolved = element.reference?.resolve() ?: element
                        var color: TextAttributesKey? = null
                        PsiTreeUtil.treeWalkUp(resolved, null) { scope, _ ->
                            color = when (scope) {
                                is FuncBlockStatement -> FuncColor.LOCAL_VARIABLE.textAttributesKey
                                is FuncConstVar -> FuncColor.CONSTANT.textAttributesKey
                                is FuncGlobalVar -> FuncColor.GLOBAL_VARIABLE.textAttributesKey
                                is FuncFunctionParameter -> FuncColor.PARAMETER.textAttributesKey
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
                }
                return
            }
        }
        val parent = element.parent
        when (parent) {
            is FuncFunction -> {
                if (element == parent.nameIdentifier) {
                    highlight(element, holder, FuncColor.FUNCTION_DECLARATION.textAttributesKey)
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
