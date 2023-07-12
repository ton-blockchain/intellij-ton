package org.ton.intellij.func.psi.impl

import com.github.weisj.jsvg.T
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.impl.source.resolve.ResolveCache
import com.intellij.psi.scope.PsiScopeProcessor
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.PairProcessor
import com.intellij.util.containers.OrderedSet
import org.ton.intellij.func.psi.*
import org.ton.intellij.func.psi.FuncPsiUtil.allowed

class FuncReference(
    element: FuncReferenceExpression,
    rangeInElement: TextRange,
) : PsiReferenceBase.Poly<FuncReferenceExpression>(element, rangeInElement, false) {
    private val resolver = ResolveCache.PolyVariantResolver<FuncReference> { t, incompleteCode ->
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

        if (!PsiTreeUtil.treeWalkUp(element, null, FuncFunctionProcessor(processor, state))) return false
        if (!processIncludeDefinitions(file, processor, state)) return false
        val stdlibFile = file.containingDirectory?.findFile("stdlib.fc")
        if (stdlibFile is FuncFile) {
            if (!processFile(stdlibFile, processor, state)) return false
        }
        if (!processFile(FuncElementFactory[file.project].builtinStdlibFile, processor, state)) return false
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
                for (child in scope.children) {
                    if (child is FuncFunction) {
                        if (!delegate.execute(child, state)) return false
                    }
                    if (child is FuncConstVariable) {
//                        println("process const: $child")
                        for (constExpression in child.expressionList) {
//                            println("process const expr ${constExpression.text}")
                            if (!processExpression(constExpression, delegate, state)) return false
                        }
                    }
                    if (child is FuncGlobalVarList) {
                        for (globalVar in child.globalVarList) {
                            if (!delegate.execute(globalVar, state)) return false
                        }
                    }
                    if (child == prevParent) return true
                }
            }
            if (scope is FuncVarExpression) return true
            if (scope is FuncBlockStatement) {
                for (statement in scope.statementList) {
                    if (statement == currentStatement) return true
                    if (!processStatement(statement, delegate, state)) return false
                }
            }
            if (scope is FuncDoStatement) {
                val block = scope.blockStatement ?: return true
                if (prevParent == block) return true
                for (statement in block.statementList) {
                    if (!processStatement(statement, delegate, state)) return false
                }
            }
            return true
        }
    }

    companion object {
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

        fun processStatement(
            statement: FuncStatement,
            processor: PsiScopeProcessor,
            state: ResolveState,
        ): Boolean {
            val expression = statement.expression
            if (!processExpression(expression, processor, state)) return false
            return true
        }

        fun processExpression(
            expression: FuncExpression?,
            processor: PsiScopeProcessor,
            state: ResolveState,
        ): Boolean {
            when (expression) {
                null -> return true
                is FuncAssignExpression -> {
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

                        is FuncTupleExpression -> {
                            for (tupleElement in left.expressionList) {
                                if (tupleElement is FuncVarExpression) {
                                    if (!processVarExpression(tupleElement, processor, state)) return false
                                }
                            }
                        }

                        is FuncReferenceExpression -> {
                            if (expression.parent is FuncConstVariable) {
                                if (!processor.execute(left, state)) return false
                            }
                        }
                    }
                    val right = expression.expressionList.getOrNull(1) ?: return true
                    if (!processExpression(right, processor, state)) return false
                }

                else -> {
                    for (funcVarExpression in PsiTreeUtil.findChildrenOfType(
                        expression,
                        FuncVarExpression::class.java
                    )) {
                        if (!processVarExpression(funcVarExpression, processor, state)) return false
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
            if (right is FuncTupleExpression) {
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
