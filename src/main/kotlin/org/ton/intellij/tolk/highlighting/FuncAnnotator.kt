package org.ton.intellij.tolk.highlighting

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.ton.intellij.tolk.TolkBundle
import org.ton.intellij.tolk.eval.TolkIntValue
import org.ton.intellij.tolk.eval.value
import org.ton.intellij.tolk.psi.*
import org.ton.intellij.tvm.math.TVM_INT_MAX_VALUE
import org.ton.intellij.tvm.math.TVM_INT_MIN_VALUE

class TolkAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        when (element) {
            is TolkTypeParameter -> {
                highlight(element.identifier, holder, TolkColor.TYPE_PARAMETER.textAttributesKey)
                return
            }
            is TolkTypeIdentifier -> {
                highlight(element.identifier, holder, TolkColor.TYPE_PARAMETER.textAttributesKey)
                return
            }
            is TolkIncludeDefinition, is TolkPragmaDefinition -> {
                val sha = element.node.firstChildNode
                val macroName = sha.treeNext


                highlight(macroName.textRange, holder, TolkColor.MACRO.textAttributesKey)
                return
            }

            is TolkGlobalVar -> {
                highlight(
                    element.identifier ?: return,
                    holder,
                    TolkColor.GLOBAL_VARIABLE.textAttributesKey
                )
                return
            }

            is TolkConstVar -> {
                highlight(
                    element.identifier ?: return,
                    holder,
                    TolkColor.CONSTANT.textAttributesKey
                )
                return
            }

            is TolkFunctionParameter -> {
                highlight(
                    element.identifier ?: return,
                    holder,
                    TolkColor.PARAMETER.textAttributesKey
                )
                return
            }

            is TolkReferenceExpression -> {
                val reference = element.reference
                if (reference == null) {
                    highlight(
                        element.identifier,
                        holder,
                        TolkColor.LOCAL_VARIABLE.textAttributesKey
                    )
                } else {
                    val resolved = reference.resolve() ?: return
                    val color = when (resolved) {
                        is TolkFunction -> TolkColor.FUNCTION_CALL
                        is TolkGlobalVar -> TolkColor.GLOBAL_VARIABLE
                        is TolkConstVar -> TolkColor.CONSTANT
                        is TolkFunctionParameter -> TolkColor.PARAMETER
                        is TolkReferenceExpression -> {
                            if (resolved.reference != null) return
                            TolkColor.LOCAL_VARIABLE
                        }

                        else -> return
                    }
                    highlight(element.identifier, holder, color.textAttributesKey)
                }
                return
            }

            is TolkLiteralExpression -> {
                val value = element.value
                if (value is TolkIntValue && (value.value < TVM_INT_MIN_VALUE || value.value > TVM_INT_MAX_VALUE)) {
                    holder.newAnnotation(
                        HighlightSeverity.ERROR,
                        TolkBundle.message("inspection.int_literal_out_of_range")
                    ).range(element).create()
                }
            }
        }
        val parent = element.parent
        when (parent) {
            is TolkFunction -> {
                if (element == parent.nameIdentifier) {
                    highlight(element, holder, TolkColor.FUNCTION_DECLARATION.textAttributesKey)
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
