package com.github.andreypfau.intellijton.tlb.ide

import com.github.andreypfau.intellijton.tlb.psi.*
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement

class TlbAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        when (element) {
            is TlbConstructorName -> holder
                .newSilentAnnotation(HighlightSeverity.INFORMATION)
                .range(element)
                .textAttributes(TlbColor.CONSTRUCTOR_NAME.textAttributesKey)
                .create()
            is TlbFieldName -> {
                holder
                    .newSilentAnnotation(HighlightSeverity.INFORMATION)
                    .range(element)
                    .textAttributes(TlbColor.FIELD_NAME.textAttributesKey)
                    .create()
            }
            is TlbImplicitFieldName -> {
                holder
                    .newSilentAnnotation(HighlightSeverity.INFORMATION)
                    .range(element)
                    .textAttributes(TlbColor.IMPLICIT_FIELD_NAME.textAttributesKey)
                    .create()
            }
            is TlbCombinatorName -> holder
                .newSilentAnnotation(HighlightSeverity.INFORMATION)
                .range(element)
                .textAttributes(TlbColor.COMBINATOR_NAME.textAttributesKey)
                .create()
            is TlbNamedRefMixin -> {
                when (element.reference.multiResolve().firstOrNull()) {
                    is TlbCombinatorName -> holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                        .range(element)
                        .textAttributes(TlbColor.TYPE.textAttributesKey)
                        .create()
                    is TlbFieldName -> holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                        .range(element)
                        .textAttributes(TlbColor.FIELD_NAME.textAttributesKey)
                        .create()
                    is TlbImplicitFieldName -> holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                        .range(element)
                        .textAttributes(TlbColor.IMPLICIT_FIELD_NAME.textAttributesKey)
                        .create()
                    else -> holder.newAnnotation(HighlightSeverity.ERROR, "Unresolved reference: ${element.text}")
                        .range(element)
                        .highlightType(ProblemHighlightType.LIKE_UNKNOWN_SYMBOL)
                        .create()
                }
            }
//            is TlbCombinator -> {
//                val parent = element.parent as TlbCombinatorDeclaration
//                val fields = parent.resolveFields()
//                element.typeExpressionList.forEach { typeExpression ->
//                    val referenceIdentifier = typeExpression.namedRef?.identifier ?: return@forEach
//                    val foundField = fields.any { field ->
//                        field.fieldName.textMatches(referenceIdentifier)
//                    }
//                    if (foundField) {
//                        holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
//                            .range(typeExpression)
//                            .textAttributes(TlbColor.FIELD_NAME.textAttributesKey)
//                            .create()
//                    } else {
//                        val implicitFields = parent.resolveImplicitFields()
//                        val foundImplicitField = implicitFields.any { implicitField ->
//                            implicitField.implicitFieldName.textMatches(referenceIdentifier)
//                        }
//                        if (foundImplicitField) {
//                            holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
//                                .range(typeExpression)
//                                .textAttributes(TlbColor.IMPLICIT_FIELD_NAME.textAttributesKey)
//                                .create()
//                        } else {
//                            holder.newAnnotation(
//                                HighlightSeverity.ERROR,
//                                "Unresolved field: ${referenceIdentifier.text}"
//                            ).range(typeExpression)
//                                .highlightType(ProblemHighlightType.LIKE_UNKNOWN_SYMBOL)
//                                .create()
//                        }
//                    }
//                }
//            }
        }
    }
}