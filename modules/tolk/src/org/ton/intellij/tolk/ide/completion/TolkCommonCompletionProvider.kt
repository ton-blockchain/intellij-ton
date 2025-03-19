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
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType
import com.intellij.util.ProcessingContext
import org.ton.intellij.tolk.TolkIcons
import org.ton.intellij.tolk.psi.*
import org.ton.intellij.tolk.psi.impl.parameters
import org.ton.intellij.tolk.sdk.TolkSdkManager
import org.ton.intellij.tolk.type.TolkFunctionType
import org.ton.intellij.util.parentOfType
import org.ton.intellij.util.psiElement

object TolkCommonCompletionProvider : TolkCompletionProvider() {
    override val elementPattern: ElementPattern<out PsiElement> =
        psiElement<PsiElement>().withParent(psiElement<TolkReferenceExpression>())

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val project = parameters.position.project
        val position = parameters.position
        val element = position.parent as TolkReferenceExpression
        val file = element.containingFile.originalFile as? TolkFile ?: return

        PsiTreeUtil.treeWalkUp(element, null) { scope, lastParent ->
            if (scope is TolkFunction) {
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

        val isDotCall = element.prevSibling?.elementType == TolkElementTypes.DOT
        val ctx = TolkCompletionContext(element)
        if (!isDotCall) {
            val function = element.parentOfType<TolkFunction>()
            if (function != null) {
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
                }.visitElement(function)
            }
        }
        file.collectIncludedFiles(true).forEach { includedFile ->
            if (!isDotCall) {
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
            file.functions.forEach {
                if (!isDotCall || (it.parameterList?.parameterList?.size ?: 0) >= 1) {
                result.addElement(
                    it.toLookupElementBuilder(ctx, true)
                )
                    }
            }
        }

        val tolkSdk = TolkSdkManager[project].getSdkRef().resolve(project)
        if (tolkSdk != null) {
            VfsUtilCore.iterateChildrenRecursively(tolkSdk.stdlibFile, null) {
                val isCommon = it.name.endsWith("common.tolk")
                val tolkFile = it.findPsiFile(project) as? TolkFile
                if (tolkFile != null) {
                    if (!isDotCall) {
                        tolkFile.constVars.forEach {
                            result.addElement(it.toLookupElementBuilder(ctx, isCommon))
                        }
                        tolkFile.globalVars.forEach {
                            result.addElement(it.toLookupElementBuilder(ctx, isCommon))
                        }
                    }
                    tolkFile.functions.forEach {
                        if (!isDotCall || (it.parameterList?.parameterList?.size ?: 0) >= 1) {
                            result.addElement(it.toLookupElementBuilder(ctx, isCommon))
                        }
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
                        .withTypeText((this.type as? TolkFunctionType)?.returnType?.let {
                            buildString {
                                it.printDisplayName(this)
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
                                        it.typeExpression?.type?.printDisplayName(this) ?: append("_")
                                    }
                                } ?: "()", true)
                        }
                        .withInsertHandler { context, item ->
                            val nextVisibleLeaf = contextElement?.let {
                                PsiTreeUtil.nextVisibleLeaf(it)
                            }

                            if (nextVisibleLeaf == null || nextVisibleLeaf.elementType != TolkElementTypes.LPAREN) {
                                context.editor.document.insertString(context.editor.caretModel.offset, "()")

                                val offset = if (this.parameters.isEmpty()) 2 else 1

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
                            typeExpression?.type?.printDisplayName(this) ?: append("_")
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
                                typeExpression?.type?.printDisplayName(this) ?: append("_")
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
                            typeExpression?.type?.printDisplayName(this) ?: append("_")
                        }),
                    TolkCompletionContributor.VAR_PRIORITY
                )
            }

            is TolkParameter -> {
                PrioritizedLookupElement.withPriority(
                    base
                        .withIcon(TolkIcons.PARAMETER)
                        .withTypeText(buildString {
                            typeExpression?.type?.printDisplayName(this) ?: append("_")
                        }),
                    TolkCompletionContributor.VAR_PRIORITY
                )
            }

            else -> LookupElementBuilder.create(this.name.toString()).withIcon(this.getIcon(0))
        }
    }
}
