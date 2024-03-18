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
import com.intellij.util.ProcessingContext
import org.ton.intellij.func.psi.*
import org.ton.intellij.func.psi.impl.isVariableDefinition
import org.ton.intellij.func.psi.impl.rawParamType
import org.ton.intellij.func.psi.impl.rawReturnType
import org.ton.intellij.func.stub.index.FuncNamedElementIndex
import org.ton.intellij.func.type.ty.FuncTyAtomic
import org.ton.intellij.func.type.ty.FuncTyTensor
import org.ton.intellij.func.type.ty.FuncTyUnit
import org.ton.intellij.util.processAllKeys
import org.ton.intellij.util.psiElement
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
        if (element.isVariableDefinition()) return
        val elementName = element.name ?: return

        val ctx = FuncCompletionContext(
            element
        )

        val processed = HashMap<String, FuncNamedElement>()

        val file = element.containingFile.originalFile as? FuncFile ?: return

        fun collectVariant(resolvedElement: FuncNamedElement): Boolean {
            val resolvedName = resolvedElement.name ?: return false
            if (processed.put(resolvedName, resolvedElement) != null) return false
            if (resolvedElement is FuncFunction) {
                println("element name: $elementName | resolvedName: $resolvedName")
                if (elementName.startsWith(".") && !resolvedName.startsWith("~")) {
                    return true
                }
                if (elementName.startsWith("~")) {
                    return if (resolvedName.startsWith("~")) {
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
                    }
                }
                return true
            }
            return false
        }

        val files = setOf(FuncPsiFactory[file.project].builtinFile) + file.collectIncludedFiles()
        files.forEach { f ->
            collectFileVariants(f) {
                if (collectVariant(it)) {
                    result.addElement(it.toLookupElementBuilder(ctx, true))
                } else {
                    println("skipped: ${it.name}")
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
}

data class FuncCompletionContext(
    val context: FuncElement?
)

fun FuncNamedElement.toLookupElementBuilder(
    context: FuncCompletionContext,
    isImported: Boolean
): LookupElement {
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
            PrioritizedLookupElement.withPriority(
                base
                    .withTypeText(this.typeReference.text)
                    .withTailText(if (includePath.isEmpty()) "" else " ($includePath)")
                    .withInsertHandler { context, item ->
                        val paramType = this.rawParamType
                        val isExtensionFunction = name.startsWith("~") || name.startsWith(".")
                        val offset = if (
                            (isExtensionFunction && paramType is FuncTyAtomic) || paramType == FuncTyUnit
                        ) 2 else 1
                        context.editor.document.insertString(context.editor.caretModel.offset, "()")
                        context.editor.caretModel.moveToOffset(context.editor.caretModel.offset + offset)
                        context.commitDocument()

                        val insertFile = context.file as? FuncFile ?: return@withInsertHandler
                        val includeCandidateFile = file as? FuncFile ?: return@withInsertHandler
                        insertFile.import(includeCandidateFile)
                    },
                if (isImported) FuncCompletionContributor.FUNCTION_PRIORITY
                else FuncCompletionContributor.NOT_IMPORTED_FUNCTION_PRIORITY
            )
        }

        else -> LookupElementBuilder.create(this.name.toString()).withIcon(this.getIcon(0))
    }
}
