package org.ton.intellij.tlb.ide

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement
import org.ton.intellij.tlb.psi.*

class TlbAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        when(element) {
            is TlbConstructor -> {
                holder.annotateInfo(element.identifier, TlbColor.CONSTRUCTOR_NAME)
            }
            is TlbCommonField -> {
                val identifier = element.identifier
                if (identifier != null) {
                    holder.annotateInfo(identifier, TlbColor.FIELD_NAME)
                }
            }
            is TlbImplicitField -> {
                if (element.typeKeyword != null) {
                    holder.annotateInfo(element.identifier, TlbColor.TYPE_PARAMETER)
                } else {
                    holder.annotateInfo(element.identifier, TlbColor.IMPLICIT_FIELD_NAME)
                }
            }
            is TlbResultType -> {
                holder.annotateInfo(element.identifier, TlbColor.CONSTRUCTOR_TYPE_NAME)
            }
            is TlbParamTypeExpression -> {
                when(val resolved = element.reference?.resolve()) {
                    is TlbImplicitField -> {
                        if (resolved.typeKeyword != null) {
                            holder.annotateInfo(element, TlbColor.TYPE_PARAMETER)
                        } else {
                            holder.annotateInfo(element, TlbColor.IMPLICIT_FIELD_NAME)
                        }
                    }
                    null -> {
                        val text = element.text
                        if (text.startsWith("uint") || text.startsWith("int") || text.startsWith("bits")) {
                            holder.annotateInfo(element, TlbColor.BUILTIN_TYPE)
                        }
                    }
                }
            }
        }
    }

    private fun AnnotationHolder.annotateInfo(element: PsiElement, color: TlbColor) {
        newSilentAnnotation(HighlightSeverity.INFORMATION)
            .range(element)
            .textAttributes(color.textAttributesKey)
            .create()
    }
}
