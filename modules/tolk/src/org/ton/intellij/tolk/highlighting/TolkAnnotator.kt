package org.ton.intellij.tolk.highlighting

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import com.intellij.psi.util.parentOfType
import org.ton.intellij.tolk.ide.colors.TolkColor
import org.ton.intellij.tolk.ide.configurable.tolkSettings
import org.ton.intellij.tolk.psi.*
import org.ton.intellij.tolk.psi.impl.hasReceiver
import org.ton.intellij.tolk.psi.impl.hasSelf
import org.ton.intellij.tolk.psi.reference.TolkStructFieldReference
import org.ton.intellij.tolk.type.TolkPrimitiveTy
import org.ton.intellij.tolk.type.TolkTyParam

class TolkAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        val elementType = element.elementType ?: return
        if (elementType == TolkElementTypes.IDENTIFIER) {
            val parent = element.parent as? TolkReferenceElement ?: return
            highlightReference(parent, holder)
        }
    }

    private fun highlightReference(
        element: TolkReferenceElement,
        holder: AnnotationHolder,
    ) {
        val referenceName = element.referenceName ?: return
        if (referenceName == "_") return
        val reference = element.reference
        if (reference == null) {
            val isPrimitiveType = TolkPrimitiveTy.fromReference(element) != null
            if (isPrimitiveType) {
                holder.info(TolkColor.PRIMITIVE.textAttributesKey)
            }
            return
        }

        val resolved = if (reference is TolkStructFieldReference) {
            if (element.parentOfType<TolkAnnotation>() != null) return
            reference.multiResolve(false).firstOrNull()?.element
        } else {
            reference.resolve()
        }
        when (resolved) {
            is TolkFunction -> {
                val color = when {
                    resolved.hasSelf -> TolkColor.METHOD_CALL.textAttributesKey
                    resolved.hasReceiver -> TolkColor.STATIC_FUNCTION_CALL.textAttributesKey
                    else -> TolkColor.FUNCTION_CALL.textAttributesKey
                }
                holder.info(color)
            }

            null -> {
                val parent = element.parent
                val isPrimitiveType = TolkPrimitiveTy.fromReference(element) != null
                if (parent is TolkMatchPattern && isPrimitiveType) {
                    return holder.info(TolkColor.PRIMITIVE.textAttributesKey)
                }
                val function = parent.parentOfType<TolkFunction>()
                if (function != null && function.resolveGenericType(referenceName)?.parameter?.psi == element
                ) {
                    return holder.info(TolkColor.TYPE_PARAMETER.textAttributesKey)
                }

                if (element !is TolkFieldLookup || parent !is TolkDotExpression || parent.expression.type !is TolkTyParam) {
                    if (element.project.tolkSettings.hasStdlib) {
                        return holder.error(
                            "Unresolved reference: `$referenceName`",
                            ProblemHighlightType.LIKE_UNKNOWN_SYMBOL
                        )
                    }
                }
            }

            else -> {
                val color = TolkDumbAnnotator.identifierColor(resolved)
                if (color != null) {
                    holder.info(color)
                }
            }
        }

    }

    private fun AnnotationHolder.info(key: TextAttributesKey) =
        newSilentAnnotation(HighlightSeverity.INFORMATION)
            .textAttributes(key)
            .create()

    private fun AnnotationHolder.error(message: String, highlightType: ProblemHighlightType? = null) =
        newAnnotation(HighlightSeverity.ERROR, message)
            .apply {
                highlightType?.let { highlightType(it) }
            }
            .create()
}
