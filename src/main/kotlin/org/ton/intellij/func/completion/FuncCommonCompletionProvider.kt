package org.ton.intellij.func.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.PsiElement
import com.intellij.psi.search.PsiElementProcessor
import com.intellij.util.ProcessingContext
import org.ton.intellij.func.psi.*
import org.ton.intellij.func.psi.impl.isVariableDefinition
import org.ton.intellij.func.psi.impl.rawParamType
import org.ton.intellij.func.stub.index.FuncNamedElementIndex
import org.ton.intellij.func.type.ty.FuncTyAtomic
import org.ton.intellij.func.type.ty.FuncTyUnit
import org.ton.intellij.util.parentOfType
import org.ton.intellij.util.processAllKeys
import org.ton.intellij.util.psiElement

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
        if (element.isVariableDefinition()) return

        val ctx = FuncCompletionContext(
            element
        )

        val processed = HashMap<String, FuncNamedElement>()
        collectVariants(element) { variant ->
            val variantName = variant.name ?: return@collectVariants true
            if (processed.put(variantName, variant) != null) return@collectVariants true
            result.addElement(
                PrioritizedLookupElement.withPriority(
                    variant.toLookupElementBuilder(ctx),
                    FuncCompletionContributor.FUNCTION_PRIORITY
                )
            )
            true
        }

        val file = element.containingFile.originalFile as? FuncFile ?: return
        val files = file.collectIncludedFiles()
        files.forEach { file ->
            collectFileVariants(file) {
                val name = it.name ?: return@collectFileVariants true
                if (processed.put(name, it) != null) return@collectFileVariants true
                if (it is FuncFunction && !(it.name?.startsWith("~") == true && element.name?.startsWith(".") == true)) {
                    result.addElement(
                        PrioritizedLookupElement.withPriority(
                            it.toLookupElementBuilder(ctx),
                            FuncCompletionContributor.FUNCTION_PRIORITY
                        )
                    )
                }
                true
            }
        }
        val functions = ArrayList<FuncFunction>()
        processAllKeys(FuncNamedElementIndex.KEY, element.project) { key ->
            FuncNamedElementIndex.findElementsByName(element.project, key).forEach {
                if (it is FuncFunction && !(it.name?.startsWith("~") == true && element.name?.startsWith(".") == true)) {
                    functions.add(it)
                }
            }
            true
        }
        functions.asSequence().sortedBy {
            VfsUtilCore.findRelativePath(file.virtualFile, it.containingFile.virtualFile, '/')?.count { c -> c == '/' }
        }.distinctBy { it.name }.forEach {
            result.addElement(
                PrioritizedLookupElement.withPriority(
                    it.toLookupElementBuilder(ctx),
                    FuncCompletionContributor.NOT_IMPORTED_FUNCTION_PRIORITY
                )
            )
        }

    }

    private fun collectVariants(element: FuncReferenceExpression, processor: PsiElementProcessor<FuncNamedElement>) {
        val function = element.parentOfType<FuncFunction>()
        function?.functionParameterList?.forEach {
            processor.execute(it)
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
}

data class FuncCompletionContext(
    val context: FuncElement?
)

fun FuncNamedElement.toLookupElementBuilder(
    context: FuncCompletionContext
): LookupElementBuilder {
    val startWithDot = context.context?.text?.startsWith('.') ?: false
    val name = if (startWithDot) "." + this.name else this.name.toString()
    val base = LookupElementBuilder.create(name).withIcon(this.getIcon(0))
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

    return when (this) {
        is FuncFunction -> {
            base
                .withTypeText(this.typeReference.text)
                .withTailText(if (includePath.isEmpty()) "" else " ($includePath)")
                .withInsertHandler { context, item ->
                    val insertFile = context.file as? FuncFile ?: return@withInsertHandler
                    val includeCandidateFile = file as? FuncFile ?: return@withInsertHandler
                    val paramType = this.rawParamType

                    val isExtensionFunction = name.startsWith("~") || name.startsWith(".")
                    val offset = if (
                        (isExtensionFunction && paramType is FuncTyAtomic) || paramType == FuncTyUnit
                    ) 2 else 1
                    context.editor.document.insertString(context.editor.caretModel.offset, "()")
                    context.editor.caretModel.moveToOffset(context.editor.caretModel.offset + offset)
                    context.commitDocument()

                    insertFile.import(includeCandidateFile)
                }
        }

        else -> LookupElementBuilder.create(this.name.toString()).withIcon(this.getIcon(0))
    }
}
