package org.ton.intellij.tolk.highlighting

import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.elementType
import com.intellij.psi.util.parentOfType
import org.ton.intellij.tolk.TolkBundle
import org.ton.intellij.tolk.ide.colors.TolkColor
import org.ton.intellij.tolk.psi.*
import org.ton.intellij.tolk.psi.impl.hasReceiver
import org.ton.intellij.tolk.psi.impl.hasSelf
import org.ton.intellij.tolk.psi.reference.TolkStructFieldReference
import org.ton.intellij.tolk.type.TolkPrimitiveTy
import org.ton.intellij.tolk.type.TolkTypeParameterTy

class TolkAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
//        when (element) {
////            is TolkLiteralExpression -> {
////                val value = element.value
////                if (value is TolkIntValue && (value.value !in TVM_INT_MIN_VALUE..TVM_INT_MAX_VALUE)) {
////                    holder.newAnnotation(
////                        HighlightSeverity.ERROR,
////                        TolkBundle.message("inspection.int_literal_out_of_range")
////                    ).range(element).create()
////                }
////            }
//            is PsiComment -> {
//                val text = element.text
//                if (text.startsWith("/*") && text.endsWith("*/")) {
//                    val commentValue = text.substring(2, text.length - 2)
//                    val nestedStart = commentValue.indexOf("/*")
//                    val nestedEnd = commentValue.lastIndexOf("*/")
//                    if (nestedStart != -1 && nestedEnd != -1) {
//                        val nestedRange =
//                            TextRange(
//                                element.textRange.startOffset + 2 + nestedStart,
//                                element.textRange.startOffset + 4 + nestedEnd
//                            )
//                        holder.newAnnotation(HighlightSeverity.ERROR, TolkBundle.message("nested_comment.description"))
//                            .range(nestedRange)
//                            .withFix(RemoveNestedComments(element, nestedRange))
//                            .create()
//                    }
//                }
//            }
//        }

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
//            return holder.error(
//                "Unknown type: `${element.text}`",
//                ProblemHighlightType.LIKE_UNKNOWN_SYMBOL
//            )
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
                    resolved.hasReceiver -> TolkColor.ASSOC_FUNCTION_CALL.textAttributesKey
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

                if (element !is TolkFieldLookup || parent !is TolkDotExpression || parent.expression.type !is TolkTypeParameterTy) {
                    return holder.error(
                        "Unresolved reference: `$referenceName`",
                        ProblemHighlightType.LIKE_UNKNOWN_SYMBOL
                    )
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

    class RemoveNestedComments(
        element: PsiComment,
        val range: TextRange
    ) : LocalQuickFixAndIntentionActionOnPsiElement(element) {
        override fun getFamilyName(): String =
            TolkBundle.message("remove.nested.comment.description")

        override fun getText(): String =
            TolkBundle.message("remove.nested.comment.description")

        override fun invoke(
            project: Project,
            file: PsiFile,
            editor: Editor?,
            startElement: PsiElement,
            endElement: PsiElement
        ) {
            val text = startElement.text
            var newText = text.substring(2, text.length - 2)
                .replace("/*", "  ")
                .replace("*/", "  ")
            newText = "/*$newText*/"
            startElement.replace(TolkPsiFactory[project].createFile(newText).findChildByClass(PsiComment::class.java)!!)
        }
    }
}
