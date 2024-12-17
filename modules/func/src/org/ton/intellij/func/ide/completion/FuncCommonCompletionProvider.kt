package org.ton.intellij.func.ide.completion

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
import org.ton.intellij.func.FuncIcons
import org.ton.intellij.func.psi.*
import org.ton.intellij.func.psi.impl.isVariableDefinition
import org.ton.intellij.func.psi.impl.rawParamType
import org.ton.intellij.func.psi.impl.rawReturnType
import org.ton.intellij.func.stub.index.FuncNamedElementIndex
import org.ton.intellij.func.type.ty.FuncTyAtomic
import org.ton.intellij.func.type.ty.FuncTyTensor
import org.ton.intellij.func.type.ty.FuncTyUnit
import org.ton.intellij.util.processAllKeys
import java.util.*

object FuncCommonCompletionProvider : FuncCompletionProvider() {
    override val elementPattern: ElementPattern<out PsiElement> =
        psiElement().withParent(psiElement<FuncReferenceExpression>())

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val position = parameters.position
        val element = position.parent as FuncReferenceExpression
        if (element.isVariableDefinition()) {
            return
        }
        val elementName = element.name ?: return

        val ctx = FuncCompletionContext(
            element
        )

        val processed = HashMap<String, FuncNamedElement>()

        val file = element.containingFile.originalFile as? FuncFile ?: return

        fun collectVariant(resolvedElement: FuncNamedElement): Boolean {
            val resolvedName = resolvedElement.name ?: return false
            if (processed.put(resolvedName, resolvedElement) != null) return false
            when (resolvedElement) {
                is FuncFunction -> {
                    if (!elementName.startsWith("~") && !resolvedName.startsWith("~")) {
                        return true
                    }
                    if (elementName.startsWith("~")) {
                        return (if (resolvedName.startsWith("~")) {
                            true
                        } else {
                            val returnType = resolvedElement.rawReturnType

                            if (returnType is FuncTyTensor && returnType.types.size == 2) {
                                val retModifyType = returnType.types.first()
                                val argType = resolvedElement.rawParamType
                                argType == retModifyType ||
                                        (argType is FuncTyTensor && argType.types.first() == retModifyType)
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

        val files = setOf(FuncPsiFactory[file.project].builtinFile) + file.collectIncludedFiles()
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
            processAllKeys(FuncNamedElementIndex.KEY, element.project) { key ->
                keys.add(key)
                true
            }
            keys.forEach { key ->
                yieldAll(FuncNamedElementIndex.findElementsByName(element.project, key))
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

    private fun collectFileVariants(file: FuncFile, processor: PsiElementProcessor<FuncNamedElement>) {
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

    private fun collectLocalVariants(element: FuncReferenceExpression, processor: (FuncNamedElement) -> Unit) {
        fun processExpression(expression: FuncExpression) {
            when {
                expression is FuncReferenceExpression && expression.isVariableDefinition() -> {
                    processor(expression)
                }

                expression is FuncBinExpression -> {
                    val left = expression.left
                    processExpression(left)
                }

                expression is FuncApplyExpression -> {
                    expression.right?.let { processExpression(it) }
                }

                expression is FuncTensorExpression -> {
                    expression.expressionList.forEach { processExpression(it) }
                }

                expression is FuncTupleExpression -> {
                    expression.expressionList.forEach { processExpression(it) }
                }
            }
        }

        fun processStatement(statement: FuncStatement) {
            when (statement) {
                is FuncExpressionStatement -> {
                    val expression = statement.expression
                    processExpression(expression)
                }
            }
        }

        PsiTreeUtil.treeWalkUp(element, null) { scope, prevParent ->
            when (scope) {
                is FuncFunction -> {
                    scope.functionParameterList.forEach {
                        processor(it)
                    }
                    return@treeWalkUp false
                }

                is FuncBlockStatement -> {
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

data class FuncCompletionContext(
    val context: FuncElement?
)

fun FuncNamedElement.toLookupElementBuilder(
    context: FuncCompletionContext,
    isImported: Boolean
): LookupElement {
    val contextElement = context.context
    val contextText = contextElement?.text ?: ""
    var name = this.name ?: ""
    if (this is FuncFunction) {
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
        is FuncFunction -> {
            PrioritizedLookupElement.withPriority(
                base
                    .withTypeText(this.rawReturnType.toString())
                    .withInsertHandler { context, item ->
                        val paramType = this.rawParamType
                        val isExtensionFunction = name.startsWith("~") || name.startsWith(".")
                        val offset = if (
                            (isExtensionFunction && paramType is FuncTyAtomic) || paramType == FuncTyUnit
                        ) 2 else 1
                        val parent = contextElement?.parent
                        if (parent is FuncApplyExpression && parent.left == contextElement && (parent.right is FuncTensorExpression || parent.right is FuncUnitExpression)) {
                            context.editor.caretModel.moveToOffset(
                                context.editor.caretModel.offset + ((parent.right?.textLength) ?: 0)
                            )
                        } else {
                            if (parent !is FuncApplyExpression || parent.left != contextElement || parent.right !is FuncApplyExpression) {
                                context.editor.document.insertString(context.editor.caretModel.offset, "()")
                                context.editor.caretModel.moveToOffset(context.editor.caretModel.offset + offset)
                            }
                        }
                        context.commitDocument()

                        val insertFile = context.file as? FuncFile ?: return@withInsertHandler
                        val includeCandidateFile = file as? FuncFile ?: return@withInsertHandler
                        insertFile.import(includeCandidateFile)
                    },
                if (isImported) FuncCompletionContributor.FUNCTION_PRIORITY
                else FuncCompletionContributor.NOT_IMPORTED_FUNCTION_PRIORITY
            )
        }

        is FuncConstVar -> {
            PrioritizedLookupElement.withPriority(
                base
                    .withTypeText(intKeyword?.text ?: sliceKeyword?.text ?: "")
                    .withTailText(if (includePath.isEmpty()) "" else " ($includePath)")
                    .withInsertHandler { context, item ->
                        context.commitDocument()

                        val insertFile = context.file as? FuncFile ?: return@withInsertHandler
                        val includeCandidateFile = file as? FuncFile ?: return@withInsertHandler
                        insertFile.import(includeCandidateFile)
                    },
                if (isImported) FuncCompletionContributor.VAR_PRIORITY
                else FuncCompletionContributor.NOT_IMPORTED_VAR_PRIORITY
            )
        }

        is FuncGlobalVar -> {
            PrioritizedLookupElement.withPriority(
                base
                    .withTypeText(typeReference.text)
                    .withTailText(if (includePath.isEmpty()) "" else " ($includePath)")
                    .withInsertHandler { context, item ->
                        context.commitDocument()

                        val insertFile = context.file as? FuncFile ?: return@withInsertHandler
                        val includeCandidateFile = file as? FuncFile ?: return@withInsertHandler
                        insertFile.import(includeCandidateFile)
                    },
                if (isImported) FuncCompletionContributor.VAR_PRIORITY
                else FuncCompletionContributor.NOT_IMPORTED_VAR_PRIORITY
            )
        }

        is FuncReferenceExpression -> {
            PrioritizedLookupElement.withPriority(
                base
                    .withIcon(FuncIcons.VARIABLE)
                    .withTypeText((parent as? FuncApplyExpression)?.left?.text ?: ""),
                FuncCompletionContributor.VAR_PRIORITY
            )
        }

        is FuncFunctionParameter -> {
            PrioritizedLookupElement.withPriority(
                base
                    .withIcon(FuncIcons.PARAMETER)
                    .withTypeText(typeReference?.text ?: ""),
                FuncCompletionContributor.VAR_PRIORITY
            )
        }

        else -> LookupElementBuilder.create(this.name.toString()).withIcon(this.getIcon(0))
    }
}
