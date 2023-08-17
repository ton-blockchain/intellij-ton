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

    val identifier: PsiElement get() = element.identifier

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        if (!myElement.isValid) return ResolveResult.EMPTY_ARRAY
        return ResolveCache.getInstance(myElement.project).resolveWithCaching(this, resolver, false, incompleteCode)
    }

    override fun handleElementRename(newElementName: String): PsiElement {
        identifier.replace(FuncPsiFactory[element.project].createIdentifierFromText(newElementName))
        return element
    }

    private fun createResolveProcessor(result: MutableCollection<ResolveResult>): PsiScopeProcessor {
        return PsiScopeProcessor { element, state ->
            if (element == myElement) return@PsiScopeProcessor !result.add(PsiElementResolveResult(element))

            val name = (element as? FuncNamedElement)?.name ?: return@PsiScopeProcessor true
            val elementName = myElement.identifier

            when {
                elementName.textMatches(name) -> {
                    result.add(PsiElementResolveResult(element))
                    false
                }

                element is FuncFunction && name.firstOrNull() != '~' && elementName.textMatches("~$name") -> {
                    result.add(PsiElementResolveResult(element))
                    false
                }

                else -> true
            }
        }
    }

    fun processResolveVariants(
        processor: PsiScopeProcessor,
        implicitStdlib: Boolean = true,
    ): Boolean {
        val file = myElement.containingFile
        if (file !is FuncFile) return false
        val state = ResolveState.initial()
        val processedFiles = HashSet<String>()

        if (!PsiTreeUtil.treeWalkUp(element, null, FuncFunctionProcessor(processor, state))) return false
        if (!processIncludeDefinitions(file, processor, state, processedFiles)) return false
        if (implicitStdlib && processedFiles.none { it.endsWith("stdlib.fc") }) {
            val stdlibFile = file.originalFile.containingDirectory?.findFile("stdlib.fc")
            if (stdlibFile is FuncFile) {
                if (!processFile(stdlibFile, processor, state, processedFiles)) return false
            }
        }
        if (!processFile(FuncPsiFactory[file.project].builtinStdlibFile, processor, state, HashSet())) return false
        return true
    }

    private class FuncFunctionProcessor(val delegate: PsiScopeProcessor, val state: ResolveState) :
        PairProcessor<PsiElement, PsiElement> {
        var currentFunction: FuncFunction? = null
        var currentStatement: FuncStatement? = null

        override fun process(scope: PsiElement?, prevParent: PsiElement?): Boolean {
            if (scope is FuncFunction) {
                currentFunction = scope
                val parameterList = scope.functionParameterList
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
                    if (child is FuncConstVarList) {
                        for (constVar in child.constVarList) {
                            if (!delegate.execute(constVar, state)) return false
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
            if (scope is FuncCatch) {
                val expression = scope.expression ?: return true
                val processResult = PsiTreeUtil.processElements(expression) {
                    delegate.execute(it, state)
                }
                if (!processResult) return false
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
            for (element in elements) {
                if (!condition(element)) continue
                if (!element.isValid || !allowed(element.containingFile, null)) {
                    continue
                }
                if (!processor.execute(element, state)) return false
            }
            return true
        }

        fun processStatement(
            statement: FuncStatement,
            processor: PsiScopeProcessor,
            state: ResolveState,
        ): Boolean {
            val expression = (statement as? FuncExpressionStatement)?.expression
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
                            if (expression.parent is FuncConstVar) {
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
            return PsiTreeUtil.processElements(right) {
                if (!processor.execute(it, state)) return@processElements false
                true
            }
        }
    }

    private fun processFile(
        file: FuncFile,
        processor: PsiScopeProcessor,
        state: ResolveState,
        processedFiles: MutableSet<String>,
    ): Boolean {
//        println("processing file: ${file.name}")
        if (file == element.containingFile) return true
        if (!processNamedElements(processor, state, file.functions)) return false
        if (!processNamedElements(processor, state, file.constVars)) return false
        if (!processNamedElements(processor, state, file.globalVars)) return false
        if (!processIncludeDefinitions(file, processor, state, processedFiles)) return false
        return true
    }

    private fun processIncludeDefinitions(
        file: FuncFile,
        processor: PsiScopeProcessor,
        state: ResolveState,
        processedFiles: MutableSet<String>,
    ): Boolean {
//        println("processing include defs: ${file.includeDefinitions.size}")
        for (includeDefinition in file.includeDefinitions) {
//            println("include references: ${includeDefinition.references.size}")
            val fileReference = includeDefinition.references.lastOrNull()
            val resolvedFile = fileReference?.resolve()
            if (resolvedFile !is FuncFile) continue
//            println("resolved: ${resolvedFile.virtualFile.path}")
            if (processedFiles.add(resolvedFile.virtualFile.path)) {
                processFile(resolvedFile, processor, state, processedFiles)
            }
        }
        return true
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FuncReference) return false
        if (element != other.element) return false
        return true
    }

    override fun hashCode(): Int = element.hashCode()
}
