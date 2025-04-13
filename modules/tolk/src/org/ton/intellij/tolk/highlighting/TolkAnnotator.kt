package org.ton.intellij.tolk.highlighting

import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.CodeInsightColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.elementType
import org.ton.intellij.tolk.TolkBundle
import org.ton.intellij.tolk.eval.TolkIntValue
import org.ton.intellij.tolk.eval.value
import org.ton.intellij.tolk.psi.*
import org.ton.intellij.tolk.psi.impl.isDeprecated
import org.ton.intellij.tolk.psi.impl.isPrimitive
import org.ton.intellij.tolk.type.TolkType
import org.ton.intellij.util.TVM_INT_MAX_VALUE
import org.ton.intellij.util.TVM_INT_MIN_VALUE

class TolkAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        when (element) {
            is TolkTypeParameter -> {
                return highlight(element.identifier, holder, TolkColor.TYPE_PARAMETER.textAttributesKey)
            }

            is TolkAnnotation -> {
                val startOffset = element.at.textRange.startOffset
                val endOffset = element.identifier?.textRange?.endOffset ?: return
                return highlight(TextRange(startOffset, endOffset), holder, TolkColor.ANNOTATION.textAttributesKey)
            }

            is TolkReferenceTypeExpression -> {
                if (element.isPrimitive) {
                    return highlight(element, holder, TolkColor.PRIMITIVE.textAttributesKey)
                }
                val resolved = element.reference?.resolve()
                val color = when {
                    resolved is TolkTypeParameter -> TolkColor.TYPE_PARAMETER
                    resolved is TolkParameter && resolved.name == "self" -> TolkColor.SELF_PARAMETER
                    else -> TolkColor.IDENTIFIER
                }
                highlight(element.identifier, holder, color.textAttributesKey)
            }

            is TolkMatchPatternReference -> {
                val identifier = element.identifier
                val name = identifier.text
                if (TolkType.byName(name) != null) {
                   return highlight(identifier, holder, TolkColor.PRIMITIVE.textAttributesKey)
                }
                val resolved = element.reference?.resolve()
                val color = when(resolved) {
                    is TolkTypeParameter -> TolkColor.TYPE_PARAMETER
                    is TolkParameter -> if (resolved.name == "self") TolkColor.SELF_PARAMETER else null
                    is TolkConstVar -> TolkColor.CONSTANT
                    is TolkGlobalVar -> TolkColor.GLOBAL_VARIABLE
                    else -> null
                } ?: TolkColor.IDENTIFIER
                highlight(identifier, holder, color.textAttributesKey)
            }

            is TolkIncludeDefinition -> {
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

            is TolkParameter -> {
                highlight(
                    element.identifier,
                    holder,
                    TolkColor.PARAMETER.textAttributesKey
                )
                return
            }

            is TolkStructField -> {
                highlight(element.identifier, holder, TolkColor.FIELD.textAttributesKey)
            }

            is TolkStructExpressionField -> {
                if (element.node.findChildByType(TolkElementTypes.COLON) != null) {
                    highlight(element.identifier, holder, TolkColor.FIELD.textAttributesKey)
                }
            }

            is TolkVar -> {
                highlight(element.identifier, holder, TolkColor.LOCAL_VARIABLE.textAttributesKey)
            }

            is TolkCatchParameter -> {
                highlight(element.identifier, holder, TolkColor.LOCAL_VARIABLE.textAttributesKey)
            }

            is TolkReferenceExpression -> highlightReference(element, holder)

            is TolkLiteralExpression -> {
                val value = element.value
                if (value is TolkIntValue && (value.value < TVM_INT_MIN_VALUE || value.value > TVM_INT_MAX_VALUE)) {
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
        if (element.elementType == TolkElementTypes.GET_KEYWORD) {
            return highlight(element, holder, TolkColor.KEYWORD.textAttributesKey)
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

    fun highlightReference(element: TolkReferenceExpression, holder: AnnotationHolder) {
        val identifier = element.identifier
        if (element.name == "__expect_type") {
            highlight(identifier, holder, DefaultLanguageHighlighterColors.LABEL)
            return
        }
        val reference = element.reference ?: return
        val resolved = reference.resolve() ?: return
        val color = when (resolved) {
            is TolkFunction -> TolkColor.FUNCTION_CALL
            is TolkGlobalVar -> TolkColor.GLOBAL_VARIABLE
            is TolkConstVar -> TolkColor.CONSTANT
            is TolkParameter -> TolkColor.PARAMETER
            is TolkReferenceExpression -> {
                if (resolved.reference != null) return
                TolkColor.LOCAL_VARIABLE
            }
            is TolkVar, is TolkCatchParameter -> TolkColor.LOCAL_VARIABLE
            is TolkStructField -> TolkColor.FIELD
            else -> return
        }
        highlight(identifier, holder, color.textAttributesKey)
        if (resolved is TolkFunction && resolved.isDeprecated) {
            highlight(identifier, holder, CodeInsightColors.DEPRECATED_ATTRIBUTES)
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
