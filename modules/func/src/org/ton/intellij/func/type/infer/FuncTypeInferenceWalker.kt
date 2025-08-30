package org.ton.intellij.func.type.infer

import com.intellij.psi.PsiElementResolveResult
import org.ton.intellij.func.psi.*
import org.ton.intellij.func.psi.impl.rawType
import org.ton.intellij.func.resolve.FuncGlobalLookup
import org.ton.intellij.func.type.ty.*
import org.ton.intellij.util.infiniteWith
import org.ton.intellij.util.parentOfType
import java.util.ArrayDeque
import java.util.HashMap
import kotlin.collections.last

class FuncTypeInferenceWalker(
    val ctx: FuncInferenceContext,
    private val returnTy: FuncTy,
) {
    private val globalLookup = FuncGlobalLookup(ctx.project)
    private val currentScope = LocalSymbolsScopes()
    private var nextReferenceExpressionIsVariable = false

    fun inferFunction(func: FuncFunction): FuncTy = currentScope.useScope {
        for (parameter in func.functionParameterList) {
            currentScope.addLocalSymbol(parameter)
        }

        val body = func.blockStatement ?: return FuncTyUnknown
        return inferFunctionBody(body)
    }

    fun inferFunctionBody(block: FuncBlockStatement): FuncTy {
        return block.inferType(Expectation.ExpectHasTy(returnTy))
    }

    private fun FuncBlockStatement.inferType(
        expected: Expectation = Expectation.NoExpectation,
    ): FuncTy {
        currentScope.useScope {
            for (statement in statementList) {
                statement.inferType()
            }
        }

        return if (expected is Expectation.ExpectHasTy) {
            expected.ty
        } else {
            FuncTyUnknown
        }
    }

    private fun FuncStatement.inferType() {
        when (this) {
            is FuncReturnStatement     -> inferType()
            is FuncBlockStatement      -> inferType()
            is FuncRepeatStatement     -> inferType()
            is FuncIfStatement         -> inferType()
            is FuncDoStatement         -> inferType()
            is FuncWhileStatement      -> inferType()
            is FuncTryStatement        -> inferType()
            is FuncExpressionStatement -> expression.inferType()
            else                       -> {}
        }
    }

    private fun FuncExpression.inferTypeCoercibleTo(
        expected: FuncTy,
    ): FuncTy {
        val inferred = inferType(expected.maybeHasType())
        // TODO: coerce to expected
        return inferred
    }

    private fun FuncExpression.inferType(expected: FuncTy?) =
        inferType(expected.maybeHasType())

    private fun FuncExpression.inferType(
        expected: Expectation = Expectation.NoExpectation,
    ): FuncTy {
        if (ctx.isTypeInferred(this)) {
            return ctx.getExprTy(this)
        }
        val ty = when (this) {
            is FuncLiteralExpression       -> inferLiteral(this)
            is FuncParenExpression         -> inferType(expected)
            is FuncTensorExpression        -> inferType(expected)
            is FuncUnitExpression          -> FuncTyUnit
            is FuncTupleExpression         -> inferType(expected)
            is FuncBinExpression           -> inferType(expected)
            is FuncApplyExpression         -> inferType(expected)
            is FuncSpecialApplyExpression  -> inferType(expected)
            is FuncReferenceExpression     -> inferType(expected)
            is FuncInvExpression           -> inferType(expected)
            is FuncTernaryExpression       -> inferType(expected)
            is FuncUnaryMinusExpression    -> inferType(expected)
            is FuncPrimitiveTypeExpression -> rawType
            else                           -> FuncTyUnknown
        }?.getFuncTy() ?: FuncTyUnknown

        ctx.setExprTy(this, ty)

        return ty
    }

    private fun inferLiteral(expr: FuncLiteralExpression): FuncTy? {
        // TODO: optimize via stubs
        if (expr.integerLiteral != null || expr.trueKeyword != null || expr.falseKeyword != null) {
            return FuncTyInt
        } else if (expr.stringLiteral != null) {
            return FuncTySlice
        }
        return null
    }

    private fun FuncReturnStatement.inferType(): FuncTy {
        return expression?.inferTypeCoercibleTo(returnTy) ?: FuncTyUnknown
    }

    private fun FuncRepeatStatement.inferType(): FuncTy {
        expression?.inferType(FuncTyInt)
        blockStatement?.inferType()
        return FuncTyUnit
    }

    private fun FuncIfStatement.inferType(): FuncTy {
        condition?.inferType(FuncTyInt)
        blockStatement?.inferType()
        elseBranch?.statement?.inferType()
        return FuncTyUnit
    }

    private fun FuncDoStatement.inferType(): FuncTy {
        val blockStatement = blockStatement ?: return FuncTyUnit
        currentScope.useScope {
            for (statement in blockStatement.statementList) {
                statement.inferType()
            }

            // condition can use variables defined in the body
            condition?.inferType(FuncTyInt)
        }
        return FuncTyUnit
    }

    private fun FuncWhileStatement.inferType(): FuncTy {
        condition?.inferType(FuncTyInt)
        blockStatement?.inferType()
        return FuncTyUnit
    }

    private fun FuncTryStatement.inferType(): FuncTy {
        blockStatement?.inferType()

        (catch?.expression as? FuncTensorExpression)?.let { tensor ->
            tensor.expressionList.forEach { tensorElement ->
                if (tensorElement is FuncReferenceExpression) {
                    currentScope.addLocalSymbol(tensorElement)
                }
            }
        }

        catch?.expression?.inferType(
            FuncTyTensor(
                FuncTyVar(),
                FuncTyInt
            )
        )
        catch?.blockStatement?.inferType()
        return FuncTyUnit
    }

    private fun FuncTensorExpression.inferType(
        expected: Expectation,
    ): FuncTy {
        val expressions = expressionList
        if (expressions.isEmpty()) {
            return FuncTyUnit
        }
        val types = expressions.inferType(null)
        if (types.size == 1) {
            return types[0]
        }
        return FuncTyTensor(types)
    }

    private fun FuncTupleExpression.inferType(
        expected: Expectation,
    ): FuncTy {
        return FuncTyTuple(expressionList.inferType(null))
    }

    private fun FuncParenExpression.inferType(
        expected: Expectation,
    ): FuncTy {
        return expression?.inferType(expected) ?: FuncTyUnknown
    }

    private fun FuncBinExpression.inferType(
        expected: Expectation,
    ): FuncTy {
        val left = left
        val leftTy = left.inferType()
        val rightTy = right?.inferTypeCoercibleTo(leftTy)

        if (binaryOp.eq != null) {
            if (left is FuncApplyExpression && left.left is FuncHoleTypeExpression) {
                // var a = ...
                ctx.setExprTy(left.right, rightTy)
            }

            val variables = (left as? FuncApplyExpression)?.right as? FuncTensorExpression ?: left as? FuncTensorExpression
            if (variables != null && rightTy is FuncTyTensor) {
                processVariables(variables.expressionList, rightTy.types)
            }

            val tupleVariables = (left as? FuncApplyExpression)?.right as? FuncTupleExpression ?: left as? FuncTupleExpression
            if (tupleVariables != null && rightTy is FuncTyTuple) {
                processVariables(tupleVariables.expressionList, rightTy.types)
            }
        }

        return rightTy ?: leftTy
    }

    private fun processVariables(variables: List<FuncExpression?>, rightTypes: List<FuncTy>) {
        for ((index, expr) in variables.withIndex()) {
            val type = rightTypes.getOrNull(index) ?: break

            // (var a, var b) = ...
            //  ^^^^^^ set type of this
            // (a, b) = ...
            //  ^ or set type of this
            ctx.setExprTy(expr, type)
            if (expr is FuncApplyExpression && expr.left is FuncHoleTypeExpression) {
                // (var a, var b) = ...
                //      ^ set type of this
                ctx.setExprTy(expr.right, type)
            }
        }
    }

    private fun FuncApplyExpression.inferType(
        expected: Expectation,
    ): FuncTy {
        val expressions = expressionList
        val lhs = expressions[0]
        val rhs = expressions.getOrNull(1)

        fun FuncExpression.isTypeExpression(): Boolean {
            return when (this) {
                is FuncPrimitiveTypeExpression,
                is FuncHoleTypeExpression,
                                        -> true

                is FuncTupleExpression  -> expressionList.all { it.isTypeExpression() }
                is FuncTensorExpression -> expressionList.all { it.isTypeExpression() }
                else                    -> false
            }
        }

        if (lhs.isTypeExpression()) {
            nextReferenceExpressionIsVariable = true
            val lhsTy = lhs?.inferType()
            rhs?.inferType()
            if (rhs != null && lhsTy != null) {
                ctx.setExprTy(rhs, lhsTy)
            }
            nextReferenceExpressionIsVariable = false
            return FuncTyUnit
        }

        val funcTy = lhs?.inferType()
        rhs?.inferType()

        return when (funcTy) {
            is FuncTyMap -> {
                funcTy.to
            }

            else         -> FuncTyUnknown
        }
    }

    private fun FuncSpecialApplyExpression.inferType(
        expected: Expectation,
    ): FuncTy {
        val expressions = expressionList
        val lhs = expressions.getOrNull(0)
        val rhs = expressions.getOrNull(1)
        lhs?.inferType()
        val rhsTy = rhs?.inferType()

        val called = (rhs as? FuncApplyExpression)?.left
        if (called?.text?.startsWith("~") == true && rhsTy is FuncTyTensor) {
            // slice~load_int()
            // (slice, int) -> int
            return rhsTy.types.getOrNull(1) ?: FuncTyUnknown
        }

        return rhsTy ?: FuncTyUnknown
    }

    private fun FuncReferenceExpression.inferType(
        expected: Expectation,
    ): FuncTy {
        if (nextReferenceExpressionIsVariable) {
            currentScope.addLocalSymbol(this)
            return FuncTyUnknown
        }

        val resolved = resolve()?.firstOrNull() ?: return FuncTyUnknown
        return when (resolved) {
            is FuncFunction            -> resolved.rawType
            is FuncFunctionParameter   -> resolved.typeReference?.rawType ?: FuncTyUnknown
            is FuncGlobalVar           -> resolved.typeReference.rawType
            is FuncConstVar            -> {
                if (resolved.intKeyword != null)
                    FuncTyInt
                else if (resolved.sliceKeyword != null)
                    FuncTySlice
                else
                    resolved.expression?.inferType() ?: FuncTyUnknown
            }

            is FuncReferenceExpression -> ctx.getExprTy(resolved)
            else                       -> FuncTyUnknown
        }
    }

    private fun FuncReferenceExpression.resolve(): Collection<FuncNamedElement>? {
        val localSymbol = currentScope.lookupSymbol(this.text)
        if (localSymbol != null) {
            val result = listOf(localSymbol)
            ctx.setResolvedRefs(this, result.map { PsiElementResolveResult(it) })
            return result
        }

        val globalSymbols = globalLookup.resolve(this)
        if (globalSymbols != null) {
            ctx.setResolvedRefs(this, globalSymbols.map { PsiElementResolveResult(it) })
            return globalSymbols
        }

        val resolved = resolveReference(this)
        if (resolved.isNotEmpty()) {
            ctx.setResolvedRefs(this, resolved.map { PsiElementResolveResult(it) })
            return resolved
        }

        return null
    }

    private fun FuncInvExpression.inferType(
        expected: Expectation,
    ): FuncTy {
        val expression = expression
        val ty = expression?.inferType(expected)
        return ty ?: FuncTyUnknown
    }

    private fun FuncTernaryExpression.inferType(
        expected: Expectation,
    ): FuncTy {
        condition.inferType(FuncTyInt)
        val thenTy = thenBranch?.inferType(expected)
        val elseTy = elseBranch?.inferType(expected)
        return thenTy ?: elseTy ?: FuncTyUnknown
    }

    private fun FuncUnaryMinusExpression.inferType(
        expected: Expectation,
    ): FuncTy {
        val expression = expression
        val ty = expression?.inferType(expected)
        return ty ?: FuncTyUnknown
    }

    private fun List<FuncExpression>.inferType(
        expected: List<FuncTy>?,
    ): List<FuncTy> {
        val extended = expected.orEmpty().asSequence().infiniteWith(FuncTyUnknown)
        return asSequence().zip(extended).map { (expr, ty) ->
            expr.inferTypeCoercibleTo(ty)
        }.toList()
    }

    fun resolveReference(element: FuncReferenceExpression): List<FuncNamedElement> {
        if (!element.isValid) return emptyList()

        val identifier = element.identifier
        val name = identifier.text.let {
            if (it.startsWith('.')) it.substring(1) else it
        }
        val file = element.containingFile as? FuncFile
        if (file != null) {
            val contextFunction = element.parentOfType<FuncFunction>()
            val includes = file.collectIncludedFiles(true)
            includes.forEach { includedFile ->
                val result = processFile(includedFile, name, contextFunction)
                if (result != null) {
                    return result
                }
            }

            val currentDirFiles = file.parent?.files
            currentDirFiles?.mapNotNull { currentDirFile ->
                if (currentDirFile is FuncFile) {
                    val result = processFile(currentDirFile, name, contextFunction)
                    if (result != null) {
                        return result
                    }
                }
            }
        }

        return emptyList()
    }

    private fun processFile(
        includedFile: FuncFile,
        name: String,
        contextFunction: FuncFunction?,
    ): List<FuncNamedElement>? {
        includedFile.constVars.forEach { constVar ->
            if (constVar.name == name) {
                return listOf(constVar)
            }
        }
        includedFile.globalVars.forEach { globalVar ->
            if (globalVar.name == name) {
                return listOf(globalVar)
            }
        }
        includedFile.functions.forEach { function ->
            val functionName = function.name
            if (functionName == name) {
                return listOf(function)
            }
            if (name.startsWith("~") && functionName != null && !functionName.startsWith("~")) {
                val returnType = function.typeReference
                val tensorList = (returnType as? FuncTensorType)?.typeReferenceList
                if ((tensorList?.size == 2 || returnType is FuncHoleType) && functionName == name.substring(1)) {
                    return listOf(function)
                }
            }

            if (function == contextFunction) {
                return null
            }
        }

        return null
    }
}

private typealias LocalSymbolsScope = MutableMap<String, FuncNamedElement>
private typealias LocalSymbolsScopes = ArrayDeque<LocalSymbolsScope>

private fun LocalSymbolsScopes.openScope() = add(HashMap())

private fun LocalSymbolsScopes.closeScope() = removeLast()

private fun LocalSymbolsScopes.lookupSymbol(name: String?): FuncNamedElement? {
    if (name == null) return null
    val iterator = descendingIterator()
    while (iterator.hasNext()) {
        val scope = iterator.next()
        val symbol = scope.resolve(name)
        if (symbol != null) {
            return symbol
        }
    }
    return null
}

fun LocalSymbolsScope.resolve(name: String): FuncNamedElement? = this[name]

fun LocalSymbolsScope.resolve(element: FuncNamedElement): FuncNamedElement? {
    val name = element.identifier?.text ?: return null
    val parent = element.parent
    if (parent is FuncApplyExpression && parent.left == element) {
        val grandParent = parent.parent
        if (grandParent is FuncSpecialApplyExpression && grandParent.right == parent) {
            if (name.startsWith('.')) {
                return resolve(name.substring(1))
            }
            if (name.startsWith('~')) {
                return resolve(name) ?: resolve(name.substring(1))
            }
        }
    }
    return resolve(name)
}

private fun LocalSymbolsScopes.addLocalSymbol(symbol: FuncNamedElement): Boolean {
    val currentScope = last()
    val name = symbol.name ?: return false
    val result = currentScope.put(name, symbol) != null
    return result
}

private inline fun <T> LocalSymbolsScopes.useScope(block: LocalSymbolsScopes.() -> T): T {
    openScope()
    try {
        return block()
    } finally {
        closeScope()
    }
}
