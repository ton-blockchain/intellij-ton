package org.ton.intellij.tolk.ide.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.findPsiFile
import com.intellij.patterns.ElementPattern
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StubIndex
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import org.ton.intellij.tolk.TolkIcons
import org.ton.intellij.tolk.psi.*
import org.ton.intellij.tolk.psi.impl.hasSelf
import org.ton.intellij.tolk.psi.impl.parameters
import org.ton.intellij.tolk.psi.impl.toLookupElement
import org.ton.intellij.tolk.sdk.TolkSdkManager
import org.ton.intellij.tolk.stub.index.TolkFunctionIndex
import org.ton.intellij.tolk.type.TolkFunctionTy
import org.ton.intellij.util.psiElement

object TolkCommonCompletionProvider : TolkCompletionProvider() {
    override val elementPattern: ElementPattern<out PsiElement> =
        psiElement<PsiElement>()
            .withParent(
                psiElement<TolkReferenceExpression>()
                    .andNot(
                        psiElement<TolkReferenceExpression>()
                            .withParent(TolkDotExpression::class.java)
                    )
            )


    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val project = parameters.position.project
        val position = parameters.position
        val element = position.parent as TolkReferenceExpression
        val file = element.containingFile.originalFile as? TolkFile ?: return

        var currentFunction: TolkFunction? = null
        PsiTreeUtil.treeWalkUp(element, null) { scope, lastParent ->
            if (scope is TolkFunction) {
                currentFunction = scope
                scope.parameterList?.parameterList?.forEach {
                    result.addElement(
                        it.toLookupElementBuilder(TolkCompletionContext(element), true)
                    )
                }
            }
            if (scope is TolkCatch && lastParent is TolkBlockStatement) {
                scope.catchParameterList.forEach { catchParameter ->
                    result.addElement(
                        catchParameter.toLookupElementBuilder(TolkCompletionContext(element), true)
                    )
                }
            }
            if (scope is TolkInferenceContextOwner) {
                return@treeWalkUp false
            }
            if (scope is TolkBlockStatement) {
                scope.statementList.forEach { statement ->
                    if (statement == lastParent) return@treeWalkUp true
                    if (statement !is TolkExpressionStatement) return@forEach
                    val expression = statement.expression
                    if (expression is TolkVarExpression) {
                        fun addVarDefinition(varDefinition: TolkVarDefinition?) {
                            when (varDefinition) {
                                is TolkVar -> {
                                    result.addElement(
                                        varDefinition.toLookupElementBuilder(TolkCompletionContext(element), true)
                                    )
                                }

                                is TolkVarTuple -> {
                                    varDefinition.varDefinitionList.forEach {
                                        addVarDefinition(it)
                                    }
                                }

                                is TolkVarTensor -> {
                                    varDefinition.varDefinitionList.forEach {
                                        addVarDefinition(it)
                                    }
                                }
                            }
                        }

                        addVarDefinition(expression.varDefinition)
                    }
                }
            }

            true
        }

        StubIndex.getInstance().processAllKeys(TolkFunctionIndex.KEY, project) { key ->
            StubIndex.getInstance().processElements(
                TolkFunctionIndex.KEY,
                key,
                project,
                GlobalSearchScope.allScope(project),
                TolkFunction::class.java
            ) { function ->
                if (function.hasSelf) return@processElements true
                result.addElement(function.toLookupElement())
                true
            }
            true
        }

        val ctx = TolkCompletionContext(element)
        if (currentFunction != null) {
            object : TolkVisitor() {
                override fun visitParameter(o: TolkParameter) {
                    result.addElement(
                        o.toLookupElementBuilder(ctx, true)
                    )
                }

                override fun visitVar(o: TolkVar) {
                    result.addElement(
                        o.toLookupElementBuilder(ctx, true)
                    )
                }

                override fun visitCatchParameter(o: TolkCatchParameter) {
                    result.addElement(
                        o.toLookupElementBuilder(ctx, true)
                    )
                }
            }.visitElement(currentFunction)
        }
        file.collectIncludedFiles(true).forEach { includedFile ->
            file.constVars.forEach {
                result.addElement(
                    it.toLookupElementBuilder(ctx, true)
                )
            }
            file.globalVars.forEach {
                result.addElement(
                    it.toLookupElementBuilder(ctx, true)
                )
            }
        }

        val tolkSdk = TolkSdkManager[project].getSdkRef().resolve(project)
        if (tolkSdk != null) {
            VfsUtilCore.iterateChildrenRecursively(tolkSdk.stdlibFile, null) {
                val isCommon = it.name.endsWith("common.tolk")
                val tolkFile = it.findPsiFile(project) as? TolkFile
                if (tolkFile != null) {
                    tolkFile.constVars.forEach {
                        result.addElement(it.toLookupElementBuilder(ctx, isCommon))
                    }
                    tolkFile.globalVars.forEach {
                        result.addElement(it.toLookupElementBuilder(ctx, isCommon))
                    }
                }
                true
            }
        }

        result.addElement(LookupElementBuilder.create("null").bold())
        result.addElement(LookupElementBuilder.create("true").bold())
        result.addElement(LookupElementBuilder.create("false").bold())
    }

    data class TolkCompletionContext(
        val context: TolkElement?
    )

    fun TolkNamedElement.toLookupElementBuilder(
        context: TolkCompletionContext,
        isImported: Boolean
    ): LookupElement {
        val contextElement = context.context
        val name = this.name ?: ""
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

        return when (this) {
            is TolkFunction -> {
                PrioritizedLookupElement.withPriority(
                    base
                        .withTypeText((this.type as? TolkFunctionTy)?.returnType?.let {
                            buildString {
                                it.renderAppendable(this)
                            }
                        } ?: "_")
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
                        .let { builder ->
                            builder.appendTailText(
                                parameterList?.parameterList?.joinToString(
                                    prefix = "(",
                                    postfix = ")"
                                ) {
                                    buildString {
                                        append(it.name)
                                        append(": ")
                                        it.typeExpression?.type?.renderAppendable(this) ?: append("_")
                                    }
                                } ?: "()", true)
                        }
                        .withInsertHandler { context, item ->
                            val offset = context.editor.caretModel.offset
                            val chars = context.document.charsSequence

                            val absoluteOpeningBracketOffset = chars.indexOfSkippingSpace('(', offset)
                            val absoluteCloseBracketOffset =
                                absoluteOpeningBracketOffset?.let { chars.indexOfSkippingSpace(')', it + 1) }

                            if (absoluteOpeningBracketOffset == null) {
                                val offset = if (this.parameters.isEmpty()) 2 else 1
                                context.editor.document.insertString(context.editor.caretModel.offset, "()")
                                context.editor.caretModel.moveToOffset(context.editor.caretModel.offset + offset)
                                context.commitDocument()
                            }

                            if (!isImported) {
                                val insertFile = context.file as? TolkFile ?: return@withInsertHandler
                                val includeCandidateFile = file as? TolkFile ?: return@withInsertHandler
                                insertFile.import(includeCandidateFile)
                            }
                        },
                    if (isImported) TolkCompletionContributor.FUNCTION_PRIORITY
                    else TolkCompletionContributor.NOT_IMPORTED_FUNCTION_PRIORITY
                )
            }

            is TolkConstVar -> {
                PrioritizedLookupElement.withPriority(
                    base
                        .withTypeText(buildString {
                            typeExpression?.type?.renderAppendable(this) ?: append("_")
                        })
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
                        .withTypeText(
                            buildString {
                                typeExpression?.type?.renderAppendable(this) ?: append("_")
                            }
                        )
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

            is TolkVar -> {
                PrioritizedLookupElement.withPriority(
                    base
                        .withIcon(TolkIcons.VARIABLE)
                        .withTypeText(buildString {
                            typeExpression?.type?.renderAppendable(this) ?: append("_")
                        }),
                    TolkCompletionContributor.VAR_PRIORITY
                )
            }

            is TolkParameter -> {
                PrioritizedLookupElement.withPriority(
                    base
                        .withIcon(TolkIcons.PARAMETER)
                        .withTypeText(buildString {
                            typeExpression?.type?.renderAppendable(this) ?: append("_")
                        }),
                    TolkCompletionContributor.VAR_PRIORITY
                )
            }

            else -> LookupElementBuilder.create(this.name.toString()).withIcon(this.getIcon(0))
        }
    }
}

private fun CharSequence.indexOfSkippingSpace(c: Char, startIndex: Int): Int? {
    for (i in startIndex until this.length) {
        val currentChar = this[i]
        if (c == currentChar) return i
        if (currentChar != ' ' && currentChar != '\t') return null
    }

    return null
}
