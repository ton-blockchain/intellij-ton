package org.ton.intellij.func.psi.impl

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.ResolveResult
import com.intellij.psi.ResolveState
import com.intellij.psi.impl.source.resolve.ResolveCache
import com.intellij.psi.scope.PsiScopeProcessor
import com.intellij.util.containers.OrderedSet
import org.ton.intellij.func.psi.FuncFile
import org.ton.intellij.func.psi.FuncNamedElement
import org.ton.intellij.func.psi.FuncPsiUtil.allowed
import org.ton.intellij.func.psi.FuncReferenceExpression

class FuncReference<T : FuncReferenceExpression>(
    element: T,
    rangeInElement: TextRange,
) : PsiReferenceBase.Poly<T>(element, rangeInElement, false) {
    private val resolver = ResolveCache.PolyVariantResolver<FuncReference<T>> { t, incompleteCode ->
        if (!myElement.isValid) return@PolyVariantResolver ResolveResult.EMPTY_ARRAY
        val result = OrderedSet<ResolveResult>()
        val resolveProcessor = createResolveProcessor(result)
        processResolveVariants(resolveProcessor)
        result.toTypedArray()
    }

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        if (!myElement.isValid) return ResolveResult.EMPTY_ARRAY
        return ResolveCache.getInstance(myElement.project).resolveWithCaching(this, resolver, false, incompleteCode)
    }

    private fun createResolveProcessor(result: MutableCollection<ResolveResult>): PsiScopeProcessor {
        return PsiScopeProcessor { element, state ->
            if (element == myElement) return@PsiScopeProcessor !result.add(PsiElementResolveResult(element))
            val name = (element as? FuncNamedElement)?.name
            if (name != null && myElement.identifier.textMatches(name)) {
                result.add(PsiElementResolveResult(element))
                false
            } else {
                true
            }
        }
    }

    private fun processResolveVariants(processor: PsiScopeProcessor): Boolean {
        val file = myElement.containingFile
        if (file !is FuncFile) return false
        val state = ResolveState.initial()
        if (!processFile(file, processor, state)) return false
        return true
    }

    private fun processFile(
        file: FuncFile,
        processor: PsiScopeProcessor,
        state: ResolveState,
    ): Boolean {
//        println("processing file: ${file.name}")
        if (!processNamedElements(processor, state, file.functions)) return false
        if (!processIncludeDefinitions(file, processor, state)) return false
        return true
    }

    private fun <T : FuncNamedElement> processNamedElements(
        processor: PsiScopeProcessor,
        state: ResolveState,
        elements: Collection<T>,
        condition: (T) -> Boolean = { true },
    ): Boolean {
//        println("processing named elements")
        for (element in elements) {
            if (!condition(element)) continue
            if (!element.isValid || !allowed(element.containingFile, null)) continue
            if (!processor.execute(element, state)) return false
        }
        return true
    }

    private val processedFiles = HashSet<String>()

    private fun processIncludeDefinitions(
        file: FuncFile,
        processor: PsiScopeProcessor,
        state: ResolveState,
    ): Boolean {
//        println("processing include defs: ${file.includeDefinitions.size}")
        for (includeDefinition in file.includeDefinitions) {
//            println("include references: ${includeDefinition.references.size}")
            val fileReference = includeDefinition.references.lastOrNull()
            val resolvedFile = fileReference?.resolve()
            if (resolvedFile !is FuncFile) continue
//            println("resolved: ${resolvedFile.virtualFile.path}")
            if (processedFiles.add(resolvedFile.virtualFile.path)) {
                processFile(resolvedFile, processor, state)
            }
        }
        return true
    }


}
