package org.ton.intellij.tolk.ide.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.StandardPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import org.ton.intellij.tolk.TolkIcons
import org.ton.intellij.tolk.ide.completion.TolkCommonCompletionProvider.TolkCompletionContext
import org.ton.intellij.tolk.psi.*
import org.ton.intellij.tolk.psi.impl.hasSelf
import org.ton.intellij.tolk.psi.impl.toLookupElement
import org.ton.intellij.tolk.stub.index.TolkNamedElementIndex
import org.ton.intellij.tolk.type.TolkFunctionTy
import org.ton.intellij.tolk.type.TolkTy
import org.ton.intellij.tolk.type.render
import org.ton.intellij.util.psiElement

object TolkCommonCompletionProvider : TolkCompletionProvider() {
    override val elementPattern: ElementPattern<out PsiElement> =
        psiElement<PsiElement>()
            .withParent(
                psiElement<TolkReferenceExpression>()
//                    .andNot(
//                        psiElement<TolkReferenceExpression>()
//                            .withParent(TolkDotExpression::class.java)
//                    )
            )
            .andNot(
                PlatformPatterns.psiElement().afterLeaf(
                    PlatformPatterns.psiElement().withText(StandardPatterns.string().matches("\\d+"))
                )
            )


    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val position = parameters.position
        val element = position.parent as TolkReferenceExpression
        if (position != element.identifier) return

        val project = parameters.position.project
        val file = element.containingFile.originalFile as? TolkFile ?: return

        collectLocalVariables(element) { localSymbol ->
            result.addElement(localSymbol.toLookupElement())
        }

        result.addElement(
            PrioritizedLookupElement.withPriority(
                LookupElementBuilder.create("null").bold(),
                TolkCompletionPriorities.KEYWORD
            )
        )
        result.addElement(
            PrioritizedLookupElement.withPriority(
                LookupElementBuilder.create("true").bold(),
                TolkCompletionPriorities.KEYWORD
            )
        )
        result.addElement(
            PrioritizedLookupElement.withPriority(
                LookupElementBuilder.create("false").bold(),
                TolkCompletionPriorities.KEYWORD
            )
        )

        val fileResolveScope = file.resolveScope
        val addedElements = HashSet<TolkNamedElement>()
        val prefixMatcher = result.prefixMatcher

        fun processNamedElement(namedElement: TolkNamedElement, isImported: Boolean) {
            val name = namedElement.name ?: return
            if (!prefixMatcher.prefixMatches(name)) return
            if (!addedElements.add(namedElement)) return

            if (namedElement is TolkFunction && namedElement.hasSelf) return
            when (namedElement) {
                is TolkFunction -> if (!namedElement.hasSelf) {
                    result.addElement(
                        PrioritizedLookupElement.withPriority(
                            namedElement.toLookupElement(),
                            if (isImported) TolkCompletionContributor.FUNCTION_PRIORITY
                            else TolkCompletionContributor.NOT_IMPORTED_FUNCTION_PRIORITY
                        )
                    )
                }

                is TolkConstVar -> {
                    result.addElement(
                        namedElement.toLookupElementBuilder(TolkCompletionContext(element), isImported)
                    )
                }

                is TolkGlobalVar -> {
                    result.addElement(
                        namedElement.toLookupElementBuilder(TolkCompletionContext(element), isImported)
                    )
                }

                is TolkStruct -> {
                    result.addElement(
                        PrioritizedLookupElement.withPriority(
                            namedElement.toLookupElementBuilder(TolkCompletionContext(element), isImported),
                            if (isImported) TolkCompletionPriorities.IMPORTED_TYPE
                            else TolkCompletionPriorities.NOT_IMPORTED_TYPE
                        )
                    )
                }

                is TolkTypeDef -> {
                    result.addElement(
                        PrioritizedLookupElement.withPriority(
                            namedElement.toLookupElementBuilder(TolkCompletionContext(element), isImported),
                            if (isImported) TolkCompletionPriorities.IMPORTED_TYPE
                            else TolkCompletionPriorities.NOT_IMPORTED_TYPE
                        )
                    )
                }

                else -> return
            }
        }

        TolkNamedElementIndex.processAllElements(project, fileResolveScope) {
            processNamedElement(it, true)
        }
        TolkNamedElementIndex.processAllElements(project) {
            processNamedElement(it, false)
        }
    }

    data class TolkCompletionContext(
        val context: TolkElement?
    )
}

fun TolkLocalSymbolElement.toLookupElement(): LookupElement {
    return when (this) {
        is TolkParameter -> {
            return PrioritizedLookupElement.withPriority(
                this.toLookupElementBuilder(TolkCompletionContext(this), true),
                TolkCompletionPriorities.PARAMETER
            )
        }

        is TolkSelfParameter -> {
            return        PrioritizedLookupElement.withPriority(
                LookupElementBuilder.create("self").bold(),
                TolkCompletionPriorities.PARAMETER
            )
        }

        is TolkCatchParameter -> {
            return   PrioritizedLookupElement.withPriority(
                toLookupElementBuilder(TolkCompletionContext(this), true),
                TolkCompletionPriorities.LOCAL_VAR
            )
        }

        is TolkVarDefinition -> {
            val lookup =
                this.toLookupElementBuilder(TolkCompletionContext(this), true)
            PrioritizedLookupElement.withPriority(
                lookup, TolkCompletionPriorities.LOCAL_VAR
            )
        }

        else -> {
            LookupElementBuilder.create(this.name.toString()).withIcon(this.getIcon(0))
        }
    }
}

fun TolkNamedElement.toLookupElementBuilder(
    context: TolkCompletionContext,
    isImported: Boolean
): LookupElement {
    val contextElement = context.context
    val name = this.name ?: ""
    val file = this.containingFile.originalFile
    val contextFile = context.context?.containingFile?.originalFile
    var includePath = if (file == contextFile || contextFile == null) ""
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
            val returnType = (this.type as? TolkFunctionTy)?.returnType ?: TolkTy.Unknown
            PrioritizedLookupElement.withPriority(
                base
                    .withTypeText(returnType.render())
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
                                    append((it.type ?: TolkTy.Unknown).render())
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
                            val offset = if (this.parameterList?.parameterList.isNullOrEmpty()) 2 else 1
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
                    .withTypeText((type ?: TolkTy.Unknown).render())
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
                    .withTypeText((type ?: TolkTy.Unknown).render())
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
                    .withTypeText((type ?: TolkTy.Unknown).render()),
                TolkCompletionContributor.VAR_PRIORITY
            )
        }

        is TolkParameter -> {
            PrioritizedLookupElement.withPriority(
                base
                    .withIcon(TolkIcons.PARAMETER)
                    .withTypeText((type ?: TolkTy.Unknown).render()),
                TolkCompletionContributor.VAR_PRIORITY
            )
        }

        else -> LookupElementBuilder.create(this.name.toString()).withIcon(this.getIcon(0))
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

fun collectLocalVariables(
    startFrom: PsiElement,
    processor: (TolkLocalSymbolElement) -> Unit
) {
    PsiTreeUtil.treeWalkUp(startFrom, null) { scope, lastParent ->
        if (scope is TolkFunction) {
            val parameterList = scope.parameterList
            if (parameterList != null) {
                parameterList.parameterList.forEach {
                    processor(it)

                }
                parameterList.selfParameter?.let {
                    processor(it)
                }
            }
        }
        if (scope is TolkCatch && lastParent is TolkBlockStatement) {
            scope.catchParameterList.forEach { catchParameter ->
                processor(catchParameter)
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
                    fun processVarDefinition(varDefinition: TolkVarDefinition?) {
                        when (varDefinition) {
                            is TolkVar -> {
                                processor(varDefinition)
                            }

                            is TolkVarTuple -> {
                                varDefinition.varDefinitionList.forEach {
                                    processVarDefinition(it)
                                }
                            }

                            is TolkVarTensor -> {
                                varDefinition.varDefinitionList.forEach {
                                    processVarDefinition(it)
                                }
                            }
                        }
                    }
                    processVarDefinition(expression.varDefinition)
                }
            }
        }

        true
    }
}
