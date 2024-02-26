package org.ton.intellij.func.highlighting

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.psi.PsiElement
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
                val reference = element.reference
                if (reference == null) {
                    highlight(
                        element.identifier,
                        holder,
                        FuncColor.LOCAL_VARIABLE.textAttributesKey
                    )
                } else {
                    val resolved = reference.resolve() ?: return
                    val color = when (resolved) {
                        is FuncFunction -> FuncColor.FUNCTION_CALL
                        is FuncGlobalVar -> FuncColor.GLOBAL_VARIABLE
                        is FuncConstVar -> FuncColor.CONSTANT
                        is FuncFunctionParameter -> FuncColor.PARAMETER
                        is FuncReferenceExpression -> {
                            if (resolved.reference != null) return
                            FuncColor.LOCAL_VARIABLE
                        }

                        else -> return
                    }
                    highlight(element.identifier, holder, color.textAttributesKey)
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
