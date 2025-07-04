package org.ton.intellij.tolk.ide.completion

import com.intellij.codeInsight.AutoPopupController
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.editorActions.TabOutScopesTracker
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.editor.EditorModificationUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.ton.intellij.tolk.presentation.TolkPsiRenderer
import org.ton.intellij.tolk.presentation.renderParameterList
import org.ton.intellij.tolk.presentation.renderTypeExpression
import org.ton.intellij.tolk.psi.*
import org.ton.intellij.tolk.psi.impl.hasSelf
import org.ton.intellij.tolk.type.*

inline fun <reified T : PsiElement> InsertionContext.getElementOfType(strict: Boolean = false): T? =
    PsiTreeUtil.findElementOfClassAtOffset(file, tailOffset - 1, T::class.java, strict)

val InsertionContext.alreadyHasCallParens: Boolean
    get() = nextCharIs('(')

private val InsertionContext.alreadyHasAngleBrackets: Boolean
    get() = nextCharIs('<')

private val InsertionContext.alreadyHasStructBraces: Boolean
    get() = nextCharIs('{')

fun InsertionContext.nextCharIs(c: Char): Boolean =
    document.charsSequence.indexOfSkippingSpace(c, tailOffset) != null

fun CharSequence.indexOfSkippingSpace(c: Char, startIndex: Int): Int? {
    for (i in startIndex until this.length) {
        val currentChar = this[i]
        if (c == currentChar) return i
        if (currentChar != ' ' && currentChar != '\t') return null
    }
    return null
}

data class TolkCompletionContext(
    val context: TolkElement?
)

fun TolkNamedElement.toLookupElementBuilder(
    context: TolkCompletionContext,
    substitution: Substitution = Substitution.empty()
): LookupElementBuilder {
    val name = this.name ?: ""
    val file = this.containingFile.originalFile
    val contextFile = context.context?.containingFile?.originalFile
    val includePath = if (file == contextFile || contextFile == null) ""
    else {
        val contextVirtualFile = contextFile.virtualFile
        val elementVirtualFile = file.virtualFile
        if (contextVirtualFile != null && elementVirtualFile != null) {
            if (elementVirtualFile.parent.name == "tolk-stdlib") {
                "@stdlib/${elementVirtualFile.name}"
            } else {
                VfsUtilCore.findRelativePath(contextVirtualFile, elementVirtualFile, '/') ?: ""
            }
        } else {
            file.name
        }
    }
    val type = if (this is TolkTypedElement) type?.let {
        if (it.hasGenerics()) it.substitute(substitution) else it
    } else null

    val base = LookupElementBuilder.createWithSmartPointer(name, this)
        .withIcon(this.getIcon(0))
        .withStrikeoutness(this is TolkAnnotationHolder && this.annotations.hasDeprecatedAnnotation())

    return when (this) {
        is TolkFunction -> {
            val returnType = (type as? TolkTyFunction)?.returnType
            base
                .withTypeText(returnType?.render())
                .let { builder ->
                    typeParameterList?.let { list ->
                        builder.appendTailText(
                            list.typeParameterList.joinToString(
                                prefix = "<",
                                postfix = ">"
                            ) {
                                it.name.toString()
                            }, true
                        )
                    } ?: builder
                }
                .withTailText(getTailText())
                .appendTailText(getExtraTailText(), true)
                .appendTailText(if (includePath.isEmpty()) "" else " ($includePath)", false)
                .withInsertHandler { context, item ->
                    val isMethodCall = context.getElementOfType<TolkFieldLookup>() != null
                    val document = context.document
                    if (!context.alreadyHasCallParens) {
                        document.insertString(context.selectionEndOffset, "()")
                    }
                    val hasParameters = !parameterList?.parameterList.isNullOrEmpty()
                    val caretShift = if (!hasParameters && (isMethodCall || !hasSelf)) 2 else 1
                    EditorModificationUtil.moveCaretRelatively(context.editor, caretShift)
                    if (!context.alreadyHasCallParens && caretShift == 1) {
                        TabOutScopesTracker.getInstance().registerEmptyScopeAtCaret(context.editor)
                    }
                    if (hasParameters) {
                        AutoPopupController.getInstance(project)?.autoPopupParameterInfo(context.editor, this)
                    }

                    val insertFile = context.file as? TolkFile ?: return@withInsertHandler
                    val includeCandidateFile =
                        this.originalElement.containingFile as? TolkFile ?: return@withInsertHandler
                    context.commitDocument()
                    insertFile.import(includeCandidateFile)
                }
        }

        is TolkConstVar -> base
            .withTypeText(type?.render())
            .withTailText(if (includePath.isEmpty()) "" else " ($includePath)")
            .withInsertHandler { context, item ->
                context.commitDocument()

                val insertFile = context.file as? TolkFile ?: return@withInsertHandler
                val includeCandidateFile = file as? TolkFile ?: return@withInsertHandler
                insertFile.import(includeCandidateFile)
            }

        is TolkGlobalVar -> base
            .withTypeText(type?.render())
            .withTailText(if (includePath.isEmpty()) "" else " ($includePath)")
            .withInsertHandler { context, item ->
                context.commitDocument()

                val insertFile = context.file as? TolkFile ?: return@withInsertHandler
                val includeCandidateFile = file as? TolkFile ?: return@withInsertHandler
                insertFile.import(includeCandidateFile)
            }

        is TolkVar -> base
            .withTypeText(type?.render())

        is TolkParameter -> base
            .withTypeText(type?.render())

        is TolkStructField -> base.bold()
            .withTypeText(type?.render())
            .bold()

        is TolkSelfParameter -> base.bold()

        is TolkCatchParameter -> base
            .withTypeText(type?.render())

        is TolkVarDefinition -> base
            .withTypeText(type?.render())

        is TolkStruct -> base
            .appendTailText(if (includePath.isEmpty()) "" else " ($includePath)", false)
            .withInsertHandler { context, item ->
                context.commitDocument()

                val insertFile = context.file as? TolkFile ?: return@withInsertHandler
                val includeCandidateFile = file as? TolkFile ?: return@withInsertHandler
                insertFile.import(includeCandidateFile)
            }

        is TolkTypeDef -> base
            .appendTailText(if (includePath.isEmpty()) "" else " ($includePath)", false)
            .withInsertHandler { context, item ->
                context.commitDocument()

                val insertFile = context.file as? TolkFile ?: return@withInsertHandler
                val includeCandidateFile = file as? TolkFile ?: return@withInsertHandler
                insertFile.import(includeCandidateFile)
            }

        else -> base
    }
}

fun TolkPrimitiveTy.toLookupElement(): LookupElementBuilder {
    return LookupElementBuilder.create(this.render()).withBoldness(true)
}

private fun TolkFunction.getTailText(): String {
    val parameterList = parameterList ?: return "()"
    return TolkPsiRenderer().renderParameterList(parameterList)
}

private fun TolkFunction.getExtraTailText(): String {
    val receiver = functionReceiver?.typeExpression ?: return ""
    return " of ${TolkPsiRenderer().renderTypeExpression(receiver)}"
}
