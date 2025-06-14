package org.ton.intellij.tolk.highlighting

import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.CodeInsightColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.elementType
import com.intellij.psi.util.isAncestor
import com.intellij.psi.util.parentOfType
import org.ton.intellij.tolk.TolkBundle
import org.ton.intellij.tolk.eval.TolkIntValue
import org.ton.intellij.tolk.eval.value
import org.ton.intellij.tolk.ide.colors.TolkColor
import org.ton.intellij.tolk.psi.*
import org.ton.intellij.tolk.psi.impl.hasReceiver
import org.ton.intellij.tolk.psi.impl.hasSelf
import org.ton.intellij.tolk.psi.impl.isDeprecated
import org.ton.intellij.tolk.type.TolkTy
import org.ton.intellij.tolk.type.TolkTypeParameterTy
import org.ton.intellij.util.TVM_INT_MAX_VALUE
import org.ton.intellij.util.TVM_INT_MIN_VALUE

class TolkAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        val elementType = element.elementType ?: return
        when (elementType) {
            TolkElementTypes.GET_KEYWORD,
            TolkElementTypes.LAZY_KEYWORD -> {
                holder.info(TolkColor.KEYWORD.textAttributesKey)
                return
            }

            TolkElementTypes.IDENTIFIER -> {
                when (val parent = element.parent) {
                    is TolkMatchPatternReference -> {
                        val name = element.text
                        if (TolkTy.byName(name) != null) {
                            return holder.info(TolkColor.PRIMITIVE.textAttributesKey)
                        }
                        val resolved = parent.reference?.resolve()
                        if (resolved != null) {
                            val color = identifierColor(resolved)
                            if (color != null) {
                                return holder.info(color)
                            }
                        } else {
                            return holder.error("Unresolved reference: $name", ProblemHighlightType.LIKE_UNKNOWN_SYMBOL)
                        }
                    }

                    is TolkFieldLookup -> {
                        val resolvedElement = parent.reference?.resolve()
                        when (resolvedElement) {
                            is TolkStructField,
                            is TolkFunction -> {
                                val enforcedAttributes = identifierColor(resolvedElement) ?: return
                                holder.info(enforcedAttributes)
                            }
                            null -> {
                                val dotExpr = parent.parent as? TolkDotExpression
                                if (dotExpr == null || dotExpr.expression.type !is TolkTypeParameterTy) {
                                    holder.error(
                                        "Unresolved member: ${element.text}",
                                        ProblemHighlightType.LIKE_UNKNOWN_SYMBOL
                                    )
                                }
                            }
                        }
                    }

                    is TolkReferenceTypeExpression -> {
                        val reference = parent.reference
                        if (reference == null) { // primitive type
                            holder.info(TolkColor.PRIMITIVE.textAttributesKey)
                        } else {
                            val resolvedType = reference.resolve()
                            when (resolvedType) {
                                is TolkTypeParameter -> {
                                    holder.info(TolkColor.TYPE_PARAMETER.textAttributesKey)
                                }

                                null -> {
                                    val function = parent.parentOfType<TolkFunction>()
                                    if (function != null) {
                                        val receiver = function.functionReceiver
                                        if (receiver != null && receiver.isAncestor(parent, true)) {
                                            return holder.info(TolkColor.TYPE_PARAMETER.textAttributesKey)
                                        }
                                    }

                                    reference.resolve()

                                    holder.error(
                                        "Unresolved type: ${element.text}",
                                        ProblemHighlightType.LIKE_UNKNOWN_SYMBOL
                                    )
                                }
                            }
                        }
                    }

                    is TolkReferenceExpression -> {
                        val reference = parent.reference ?: return
                        val resolved = reference.resolve() ?: return holder.error(
                            "Unresolved reference: ${element.text}",
                            ProblemHighlightType.LIKE_UNKNOWN_SYMBOL
                        )
                        val color = identifierColor(resolved)
                        if (color != null) {
                            holder.info(color)
                        }
                        val isDeprecated = (resolved is TolkFunction && resolved.isDeprecated) ||
                                (resolved is TolkTypeDef && resolved.isDeprecated) ||
                                (resolved is TolkStruct && resolved.isDeprecated) ||
                                (resolved is TolkGlobalVar && resolved.isDeprecated) ||
                                (resolved is TolkConstVar && resolved.isDeprecated)
                        if (isDeprecated) {
                            holder.info(CodeInsightColors.DEPRECATED_ATTRIBUTES)
                        }
                    }

                    else -> {
                        val color = identifierColor(parent)
                        if (color != null) {
                            holder.info(color)
                        }
                    }
                }
                return
            }
        }

        when (element) {
            is TolkLiteralExpression -> {
                val value = element.value
                if (value is TolkIntValue && (value.value !in TVM_INT_MIN_VALUE..TVM_INT_MAX_VALUE)) {
                    holder.newAnnotation(
                        HighlightSeverity.ERROR,
                        TolkBundle.message("inspection.int_literal_out_of_range")
                    ).range(element).create()
                }
            }

            is PsiComment -> {
                val text = element.text
                if (text.startsWith("/*") && text.endsWith("*/")) {
                    val commentValue = text.substring(2, text.length - 2)
                    val nestedStart = commentValue.indexOf("/*")
                    val nestedEnd = commentValue.lastIndexOf("*/")
                    if (nestedStart != -1 && nestedEnd != -1) {
                        val nestedRange =
                            TextRange(
                                element.textRange.startOffset + 2 + nestedStart,
                                element.textRange.startOffset + 4 + nestedEnd
                            )
                        holder.newAnnotation(HighlightSeverity.ERROR, TolkBundle.message("nested_comment.description"))
                            .range(nestedRange)
                            .withFix(RemoveNestedComments(element, nestedRange))
                            .create()
                    }
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

    private fun identifierColor(element: PsiElement): TextAttributesKey? {
        return when (element) {
            is TolkTypeParameter -> TolkColor.TYPE_PARAMETER.textAttributesKey
            is TolkAnnotation -> TolkColor.ANNOTATION.textAttributesKey
            is TolkGlobalVar -> TolkColor.GLOBAL_VARIABLE.textAttributesKey
            is TolkConstVar -> TolkColor.CONSTANT.textAttributesKey
            is TolkStruct -> TolkColor.STRUCT.textAttributesKey
            is TolkTypeDef -> TolkColor.TYPE_ALIAS.textAttributesKey
            is TolkStructField -> TolkColor.FIELD.textAttributesKey
            is TolkStructExpressionField -> TolkColor.FIELD.textAttributesKey
            is TolkVar -> TolkColor.VARIABLE.textAttributesKey
            is TolkCatchParameter -> TolkColor.VARIABLE.textAttributesKey
            is TolkParameter -> TolkColor.PARAMETER.textAttributesKey
            is TolkFunction -> when {
                element.hasSelf -> TolkColor.METHOD.textAttributesKey
                element.hasReceiver -> TolkColor.ASSOC_FUNCTION.textAttributesKey
                else -> TolkColor.FUNCTION.textAttributesKey
            }
            else -> null
        }
    }

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
