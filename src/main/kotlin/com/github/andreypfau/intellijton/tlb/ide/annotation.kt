package com.github.andreypfau.intellijton.tlb.ide

import com.github.andreypfau.intellijton.tlb.psi.*
import com.github.andreypfau.intellijton.tlb.resolve.resolveFields
import com.github.andreypfau.intellijton.tlb.resolve.resolveImplicitFields
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
                .range(element.identifier)
                .textAttributes(TlbColor.CONSTRUCTOR_NAME.textAttributesKey)
                .create()
            is TlbNamedField -> {
                val identifier = element.identifier
                if (identifier != null) {
                    holder
                        .newSilentAnnotation(HighlightSeverity.INFORMATION)
                        .range(identifier)
                        .textAttributes(TlbColor.FIELD_NAME.textAttributesKey)
                        .create()
                }
            }
            is TlbCombinatorName -> holder
                .newSilentAnnotation(HighlightSeverity.INFORMATION)
                .range(element.identifier)
                .textAttributes(TlbColor.COMBINATOR_NAME.textAttributesKey)
                .create()
            is TlbTypeExpression -> {
                val identifier = element.identifier
                if (identifier != null && element.parent !is TlbCombinator) {
                    val text = identifier.text
                    val first = text.first()
                    if (text.length > 1 && first == first.uppercaseChar()) {
                        holder
                            .newSilentAnnotation(HighlightSeverity.INFORMATION)
                            .range(identifier)
                            .textAttributes(TlbColor.TYPE.textAttributesKey)
                            .create()
                    }
                }
            }
            is TlbCombinator -> {
                val parent = element.parent as TlbCombinatorDeclaration
                val fields = parent.resolveFields()
                element.typeExpressionList.forEach { typeExpression ->
                    val typeExpressionIdentifier = typeExpression.identifier ?: return@forEach
                    val foundField = fields.any { field ->
                        field.identifier?.textMatches(typeExpressionIdentifier) == true
                    }
                    if (foundField) {
                        holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                            .range(typeExpression)
                            .textAttributes(TlbColor.FIELD_NAME.textAttributesKey)
                            .create()
                    } else {
                        val implicitFields = parent.resolveImplicitFields()
                        val foundImplicitField = implicitFields.any { implicitField ->
                            implicitField.identifier.textMatches(typeExpressionIdentifier)
                        }
                        if (foundImplicitField) {
                            holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                                .range(typeExpression)
                                .textAttributes(TlbColor.IMPLICIT_FIELD_NAME.textAttributesKey)
                                .create()
                        } else {
                            holder.newAnnotation(
                                HighlightSeverity.ERROR,
                                "Unresolved field: ${typeExpressionIdentifier.text}"
                            ).range(typeExpression)
                                .highlightType(ProblemHighlightType.LIKE_UNKNOWN_SYMBOL)
                                .create()
                        }
                    }
                }
            }
        }
    }
}