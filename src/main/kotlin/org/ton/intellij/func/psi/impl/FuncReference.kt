package org.ton.intellij.func.psi.impl

import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.impl.source.resolve.ResolveCache
import com.intellij.psi.scope.PsiScopeProcessor
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.PairProcessor
import com.intellij.util.containers.OrderedSet
import org.ton.intellij.func.psi.*
import org.ton.intellij.func.psi.FuncPsiUtil.allowed

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
            val name = (element as? FuncNamedElement)?.name ?: (element as? FuncReferenceExpression)?.identifier?.text
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

        if (!PsiTreeUtil.treeWalkUp(element, null, FuncFunctionProcessor(processor, state))) return false
        if (!processIncludeDefinitions(file, processor, state)) return false
        return true
    }

    private class FuncFunctionProcessor(val delegate: PsiScopeProcessor, val state: ResolveState) :
        PairProcessor<PsiElement, PsiElement> {
        var currentFunction: FuncFunction? = null
        var currentStatement: FuncStatement? = null

        override fun process(scope: PsiElement?, prevParent: PsiElement?): Boolean {
            if (scope is FuncFunction) {
                currentFunction = scope
                val parameterList = scope.functionParameterList?.functionParameterList ?: return true
                for (functionParameter in parameterList) {
                    if (!delegate.execute(functionParameter, state)) return false
                }
            }
            if (scope is FuncStatement && currentStatement == null) {
                currentStatement = scope
            }
            if (scope is FuncFile) {
                for (function in scope.functions) {
                    if (!delegate.execute(function, state)) return false
                    if (function == currentFunction) return true
                }
            }
            if (scope is FuncVarExpression) return true
            if (scope is FuncBlockStatement) {
                for (statement in scope.statementList) {
                    if (statement == currentStatement) return true
                    if (!processStatement(statement, delegate, state)) return false
                }
            }
            return true
        }
    }

    companion object {
        fun processStatement(
            statement: FuncStatement,
            processor: PsiScopeProcessor,
            state: ResolveState,
        ): Boolean {
            val expression = statement.expression
            if (expression is FuncAssignExpression) {
                val left = expression.expressionList.firstOrNull() ?: return true
                when (left) {
                    is FuncVarExpression -> {
                        if (!processVarExpression(left, processor, state)) return false
                    }

                    is FuncTensorExpression -> {
                        for (tensorElement in left.expressionList) {
                            if (tensorElement is FuncVarExpression) {
                                if (!processVarExpression(tensorElement, processor, state)) return false
                            }
                        }
                    }
                }
            }
            return true
        }

        private fun processVarExpression(
            varExpression: FuncVarExpression,
            processor: PsiScopeProcessor,
            state: ResolveState,
        ): Boolean {
            val right = varExpression.right ?: return true
            if (right is FuncReferenceExpression) {
                if (!processor.execute(right, state)) return false
            }
            if (right is FuncTensorExpression) {
                for (tensorElement in right.expressionList) {
                    when (tensorElement) {
                        is FuncVarExpression -> if (!processVarExpression(tensorElement, processor, state)) return false
                        is FuncReferenceExpression -> if (!processor.execute(tensorElement, state)) return false
                    }
                }
            }
            return true
        }
    }

    private fun processFile(
        file: FuncFile,
        processor: PsiScopeProcessor,
        state: ResolveState,
    ): Boolean {
//        println("processing file: ${file.name}")
        if (file == element.containingFile) return true
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
