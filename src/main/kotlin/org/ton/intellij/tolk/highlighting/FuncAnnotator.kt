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
import com.intellij.refactoring.suggested.startOffset
import org.ton.intellij.tolk.TolkBundle
import org.ton.intellij.tolk.eval.TolkIntValue
import org.ton.intellij.tolk.eval.value
import org.ton.intellij.tolk.psi.*
import org.ton.intellij.tolk.psi.impl.isDeprecated
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
                    DefaultLanguageHighlighterColors.VALID_STRING_ESCAPE
                    highlight(element.identifier, holder, color.textAttributesKey)
                    if (resolved is TolkFunction && resolved.isDeprecated) {
                        highlight(element.identifier, holder, CodeInsightColors.DEPRECATED_ATTRIBUTES)
                    }
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

            is PsiComment -> {
                val text = element.text
                if (text.startsWith("/*") && text.endsWith("*/")) {
                    val commentValue = text.substring(2, text.length - 2)
                    val nestedStart = commentValue.indexOf("/*")
                    val nestedEnd = commentValue.lastIndexOf("*/")
                    if (nestedStart != -1 && nestedEnd != -1) {
                        val nestedRange =
                            TextRange(element.startOffset + 2 + nestedStart, element.startOffset + 4 + nestedEnd)
                        holder.newAnnotation(HighlightSeverity.ERROR, TolkBundle.message("nested_comment.description"))
                            .range(nestedRange)
                            .withFix(RemoveNestedComments(element, nestedRange))
                            .create()
                    }
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
