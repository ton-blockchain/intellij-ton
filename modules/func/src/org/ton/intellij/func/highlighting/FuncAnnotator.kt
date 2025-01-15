package org.ton.intellij.func.highlighting

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.ton.intellij.func.FuncBundle
import org.ton.intellij.func.eval.FuncIntValue
import org.ton.intellij.func.eval.value
import org.ton.intellij.func.psi.*
import org.ton.intellij.util.TVM_INT_MAX_VALUE
import org.ton.intellij.util.TVM_INT_MIN_VALUE

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

            is FuncIncludeDefinition, is FuncPragmaDefinition -> {
                val sha = element.node.firstChildNode
                val macroName = sha.treeNext


                highlight(macroName.textRange, holder, FuncColor.MACRO.textAttributesKey)
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

            is FuncLiteralExpression -> {
                val value = element.value
                if (value is FuncIntValue && (value.value < TVM_INT_MIN_VALUE || value.value > TVM_INT_MAX_VALUE)) {
                    holder.newAnnotation(
                        HighlightSeverity.ERROR,
                        FuncBundle.message("inspection.int_literal_out_of_range")
                    ).range(element).create()
                }
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

    private fun highlight(element: PsiElement, holder: AnnotationHolder, key: TextAttributesKey) =
        highlight(element.textRange, holder, key)

    private fun highlight(textRange: TextRange, holder: AnnotationHolder, key: TextAttributesKey) {
        holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
            .range(textRange)
            .textAttributes(key)
            .create()
    }
}
