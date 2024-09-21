package org.ton.intellij.tolk.ide.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.PsiElement
import com.intellij.psi.search.PsiElementProcessor
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import org.ton.intellij.tolk.TolkIcons
import org.ton.intellij.tolk.psi.*
import org.ton.intellij.tolk.psi.impl.isVariableDefinition
import org.ton.intellij.tolk.psi.impl.rawParamType
import org.ton.intellij.tolk.psi.impl.rawReturnType
import org.ton.intellij.tolk.stub.index.TolkNamedElementIndex
import org.ton.intellij.tolk.type.ty.TolkTyAtomic
import org.ton.intellij.tolk.type.ty.TolkTyTensor
import org.ton.intellij.tolk.type.ty.TolkTyUnit
import org.ton.intellij.util.processAllKeys
import org.ton.intellij.util.psiElement
import java.util.*

object TolkCommonCompletionProvider : TolkCompletionProvider() {
    override val elementPattern: ElementPattern<out PsiElement> =
        psiElement().withParent(psiElement<TolkReferenceExpression>())

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val position = parameters.position
        val element = position.parent as TolkReferenceExpression
        if (element.isVariableDefinition()) {
            return
        }
        val elementName = element.name ?: return

        val ctx = TolkCompletionContext(
            element
        )

        val processed = HashMap<String, TolkNamedElement>()

        val file = element.containingFile.originalFile as? TolkFile ?: return

        fun collectVariant(resolvedElement: TolkNamedElement): Boolean {
            val resolvedName = resolvedElement.name ?: return false
            if (processed.put(resolvedName, resolvedElement) != null) return false
            when (resolvedElement) {
                is TolkFunction -> {
                    if (!elementName.startsWith("~") && !resolvedName.startsWith("~")) {
                        return true
                    }
                    if (elementName.startsWith("~")) {
                        return (if (resolvedName.startsWith("~")) {
                            true
                        } else {
                            val returnType = resolvedElement.rawReturnType

                            if (returnType is TolkTyTensor && returnType.types.size == 2) {
                                val retModifyType = returnType.types.first()
                                val argType = resolvedElement.rawParamType
                                argType == retModifyType ||
                                        (argType is TolkTyTensor && argType.types.first() == retModifyType)
                            } else {
                                false
                            }
                        })
                    }
                    return false
                }

                else -> return true
            }
        }

        collectLocalVariants(element) {
            if (collectVariant(it)) {
                result.addElement(it.toLookupElementBuilder(ctx, true))
            }
        }

        val files = file.collectIncludedFiles()
        files.forEach { f ->
            collectFileVariants(f) {
                if (collectVariant(it)) {
                    result.addElement(it.toLookupElementBuilder(ctx, true))
                }
                true
            }
        }
        val globalNamedElements = sequence {
            val keys = LinkedList<String>()
            processAllKeys(TolkNamedElementIndex.KEY, element.project) { key ->
                keys.add(key)
                true
            }
            keys.forEach { key ->
                yieldAll(TolkNamedElementIndex.findElementsByName(element.project, key))
            }
        }.toList()

        globalNamedElements.sortedBy {
            VfsUtilCore.findRelativePath(file.virtualFile, it.containingFile.virtualFile, '/')?.count { c -> c == '/' }
        }.forEach {
            if (collectVariant(it)) {
                result.addElement(it.toLookupElementBuilder(ctx, false))
            }
        }
    }

    private fun collectFileVariants(file: TolkFile, processor: PsiElementProcessor<TolkNamedElement>) {
        file.constVars.forEach {
            processor.execute(it)
        }
        file.globalVars.forEach {
            processor.execute(it)
        }
        file.functions.forEach {
            processor.execute(it)
        }
    }

    private fun collectLocalVariants(element: TolkReferenceExpression, processor: (TolkNamedElement) -> Unit) {
        fun processExpression(expression: TolkExpression) {
            when {
                expression is TolkReferenceExpression && expression.isVariableDefinition() -> {
                    processor(expression)
                }

                expression is TolkBinExpression -> {
                    val left = expression.left
                    processExpression(left)
                }

                expression is TolkApplyExpression -> {
                    expression.right?.let { processExpression(it) }
                }

                expression is TolkTensorExpression -> {
                    expression.expressionList.forEach { processExpression(it) }
                }

                expression is TolkTupleExpression -> {
                    expression.expressionList.forEach { processExpression(it) }
                }
            }
        }

        fun processStatement(statement: TolkStatement) {
            when (statement) {
                is TolkExpressionStatement -> {
                    val expression = statement.expression
                    processExpression(expression)
                }
            }
        }

        PsiTreeUtil.treeWalkUp(element, null) { scope, prevParent ->
            when (scope) {
                is TolkFunction -> {
                    scope.functionParameterList.forEach {
                        processor(it)
                    }
                    return@treeWalkUp false
                }

                is TolkBlockStatement -> {
                    for (funcStatement in scope.statementList) {
                        if (funcStatement == prevParent) break
                        processStatement(funcStatement)
                    }
                }
            }
            true
        }
    }
}

data class TolkCompletionContext(
    val context: TolkElement?
)

fun TolkNamedElement.toLookupElementBuilder(
    context: TolkCompletionContext,
    isImported: Boolean
): LookupElement {
    val contextElement = context.context
    val contextText = contextElement?.text ?: ""
    var name = this.name ?: ""
    if (this is TolkFunction) {
        name = when {
            contextText.startsWith('.') -> ".${this.name}"
            contextText.startsWith('~') && !name.startsWith("~") -> "~${this.name}"
            else -> name
        }
    }
    val file = this.containingFile.originalFile
    val contextFile = context.context?.containingFile?.originalFile
    val includePath = if (file == contextFile || contextFile == null) ""
    else {
        val contextVirtualFile = contextFile.virtualFile
        val elementVirtualFile = file.virtualFile
        if (contextVirtualFile != null && elementVirtualFile != null) {
            VfsUtilCore.findRelativePath(contextVirtualFile, elementVirtualFile, '/') ?: ""
        } else {
            this.containingFile.name
        }
    }
    val base = LookupElementBuilder.create(name)
        .withIcon(this.getIcon(0))
        .withTailText(if (includePath.isEmpty()) "" else " ($includePath)")

    return when (this) {
        is TolkFunction -> {
            PrioritizedLookupElement.withPriority(
                base
                    .withTypeText(this.rawReturnType.toString())
                    .withInsertHandler { context, item ->
                        val paramType = this.rawParamType
                        val isExtensionFunction = name.startsWith("~") || name.startsWith(".")
                        val offset = if (
                            (isExtensionFunction && paramType is TolkTyAtomic) || paramType == TolkTyUnit
                        ) 2 else 1
                        val parent = contextElement?.parent
                        if (parent is TolkApplyExpression && parent.left == contextElement && (parent.right is TolkTensorExpression || parent.right is TolkUnitExpression)) {
                            context.editor.caretModel.moveToOffset(
                                context.editor.caretModel.offset + ((parent.right?.textLength) ?: 0)
                            )
                        } else {
                            if (parent !is TolkApplyExpression || parent.left != contextElement || parent.right !is TolkApplyExpression) {
                                context.editor.document.insertString(context.editor.caretModel.offset, "()")
                                context.editor.caretModel.moveToOffset(context.editor.caretModel.offset + offset)
                            }
                        }
                        context.commitDocument()

                        val insertFile = context.file as? TolkFile ?: return@withInsertHandler
                        val includeCandidateFile = file as? TolkFile ?: return@withInsertHandler
                        insertFile.import(includeCandidateFile)
                    },
                if (isImported) TolkCompletionContributor.FUNCTION_PRIORITY
                else TolkCompletionContributor.NOT_IMPORTED_FUNCTION_PRIORITY
            )
        }

        is TolkConstVar -> {
            PrioritizedLookupElement.withPriority(
                base
                    .withTypeText(intKeyword?.text ?: sliceKeyword?.text ?: "")
                    .withTailText(if (includePath.isEmpty()) "" else " ($includePath)")
                    .withInsertHandler { context, item ->
                        context.commitDocument()

                        val insertFile = context.file as? TolkFile ?: return@withInsertHandler
                        val includeCandidateFile = file as? TolkFile ?: return@withInsertHandler
                        insertFile.import(includeCandidateFile)
                    },
                if (isImported) TolkCompletionContributor.VAR_PRIORITY
                else TolkCompletionContributor.NOT_IMPORTED_VAR_PRIORITY
            )
        }

        is TolkGlobalVar -> {
            PrioritizedLookupElement.withPriority(
                base
                    .withTypeText(typeReference.text)
                    .withTailText(if (includePath.isEmpty()) "" else " ($includePath)")
                    .withInsertHandler { context, item ->
                        context.commitDocument()

                        val insertFile = context.file as? TolkFile ?: return@withInsertHandler
                        val includeCandidateFile = file as? TolkFile ?: return@withInsertHandler
                        insertFile.import(includeCandidateFile)
                    },
                if (isImported) TolkCompletionContributor.VAR_PRIORITY
                else TolkCompletionContributor.NOT_IMPORTED_VAR_PRIORITY
            )
        }

        is TolkReferenceExpression -> {
            PrioritizedLookupElement.withPriority(
                base
                    .withIcon(TolkIcons.VARIABLE)
                    .withTypeText((parent as? TolkApplyExpression)?.left?.text ?: ""),
                TolkCompletionContributor.VAR_PRIORITY
            )
        }

        is TolkFunctionParameter -> {
            PrioritizedLookupElement.withPriority(
                base
                    .withIcon(TolkIcons.PARAMETER)
                    .withTypeText(typeReference?.text ?: ""),
                TolkCompletionContributor.VAR_PRIORITY
            )
        }

        else -> LookupElementBuilder.create(this.name.toString()).withIcon(this.getIcon(0))
    }
}
