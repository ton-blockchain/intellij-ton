package org.ton.intellij.tlb.ide

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement
import org.ton.intellij.tlb.psi.*

class TlbAnnotator : Annotator {

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        when (element) {
            is TlbConstructor -> {
                holder.annotateInfo(element.identifier, TlbColor.CONSTRUCTOR_NAME)
            }
            is TlbField -> {

            }
//            is TlbConstructorName -> holder.annotateInfo(element, TlbColor.CONSTRUCTOR_NAME)
//            is TlbFieldName -> holder.annotateInfo(element, TlbColor.FIELD_NAME)
//            is TlbImplicitFieldName -> holder.annotateInfo(element, TlbColor.IMPLICIT_FIELD_NAME)
//            is TlbImplicitFieldType -> holder.annotateInfo(element, TlbColor.TYPE)
//            is TlbCombinatorName -> holder.annotateInfo(element, TlbColor.COMBINATOR_NAME)
//            is TlbNamedRefMixin -> {
//                when (element.reference.multiResolve().firstOrNull()) {
//                    is TlbFieldName -> holder.annotateInfo(element, TlbColor.FIELD_NAME)
//                    is TlbNamedField -> holder.annotateInfo(element, TlbColor.FIELD_NAME)
//                    is TlbImplicitFieldName -> holder.annotateInfo(element, TlbColor.IMPLICIT_FIELD_NAME)
//                    is TlbImplicitField -> holder.annotateInfo(element, TlbColor.IMPLICIT_FIELD_NAME)
//                    is TlbCombinatorName -> holder.annotateInfo(element, TlbColor.TYPE)
//                    else -> {
//                        if (element.name in TlbParserDefinition.INBUILT_TYPE_NAMES) {
//                            holder.annotateInfo(element, TlbColor.TYPE)
//                        } else if (element.containingFile.isPhysical) {
//                            holder.annotateUnknown(element)
//                        }
//                    }
//                }
//            }
        }
    }

    private fun AnnotationHolder.annotateInfo(element: PsiElement, color: TlbColor) {
        newSilentAnnotation(HighlightSeverity.INFORMATION)
            .range(element)
            .textAttributes(color.textAttributesKey)
            .create()
    }

    private fun AnnotationHolder.annotateUnknown(element: PsiElement) {
        newAnnotation(HighlightSeverity.ERROR, "Unresolved reference: ${element.text}")
            .range(element)
            .highlightType(ProblemHighlightType.LIKE_UNKNOWN_SYMBOL)
            .create()
    }
}
