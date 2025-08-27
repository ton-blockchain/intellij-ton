package org.ton.intellij.func.type.infer

import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiElementResolveResult
import org.ton.intellij.func.psi.*
import org.ton.intellij.func.resolve.FuncGlobalLookup
import org.ton.intellij.func.type.ty.*
import org.ton.intellij.util.infiniteWith
import java.util.ArrayDeque
import java.util.HashMap
import kotlin.collections.last

class FuncTypeInferenceWalker(
    val ctx: FuncInferenceContext,
    private val returnTy: FuncTy
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
            is FuncReturnStatement -> inferType()
            is FuncBlockStatement -> inferType()
            is FuncRepeatStatement -> inferType()
            is FuncIfStatement -> inferType()
            is FuncDoStatement -> inferType()
            is FuncWhileStatement -> inferType()
            is FuncTryStatement -> inferType()
            is FuncExpressionStatement -> expression.inferType()
            else -> {}
        }
    }

    private fun FuncExpression.inferTypeCoercibleTo(
        expected: FuncTy
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
        ProgressManager.checkCanceled()
        if (ctx.isTypeInferred(this)) {
            error("Type already inferred for $this")
        }
        val ty = when (this) {
            is FuncLiteralExpression -> inferLiteral(this)
            is FuncParenExpression -> inferType(expected)
            is FuncTensorExpression -> inferType(expected)
            is FuncTupleExpression -> inferType(expected)
            is FuncBinExpression -> inferType(expected)
            is FuncApplyExpression -> inferType(expected)
            is FuncSpecialApplyExpression -> inferType(expected)
            is FuncReferenceExpression -> inferType(expected)
            is FuncInvExpression -> inferType(expected)
            is FuncTernaryExpression -> inferType(expected)
            is FuncUnaryMinusExpression -> inferType(expected)
            else -> FuncTyUnknown
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
        expected: Expectation
    ): FuncTy {
//        val fields = expected.onlyHasTy(ctx)?.let {
//            (it as? FuncTyTensor)?.types
////            (resolveTypeVarsWithObligations(it) as? FuncTyTensor)?.types TODO implement resolveTypeVarsWithObligations
//        }
        return FuncTyTensor(expressionList.inferType(null))
    }

    private fun FuncTupleExpression.inferType(
        expected: Expectation
    ): FuncTy {
        return FuncTy(expressionList.inferType(null))
    }

    private fun FuncParenExpression.inferType(
        expected: Expectation
    ): FuncTy {
        return expression?.inferType(expected) ?: FuncTyUnknown
    }

    private fun FuncBinExpression.inferType(
        expected: Expectation
    ): FuncTy {
        val leftTy = left.inferType()
        val rightTy = right?.inferTypeCoercibleTo(leftTy)
        return rightTy ?: leftTy
    }

    private fun FuncApplyExpression.inferType(
        expected: Expectation
    ): FuncTy {
        val expressions = expressionList
        val lhs = expressions[0]
        val rhs = expressions.getOrNull(1)

        fun FuncExpression.isTypeExpression(): Boolean {
            return when (this) {
                is FuncPrimitiveTypeExpression,
                is FuncHoleTypeExpression -> true

                is FuncTupleExpression -> expressionList.all { it.isTypeExpression() }
                is FuncTensorExpression -> expressionList.all { it.isTypeExpression() }
                else -> false
            }
        }

        if (lhs.isTypeExpression()) {
            nextReferenceExpressionIsVariable = true
        }
        lhs?.inferType()
        rhs?.inferType()
        nextReferenceExpressionIsVariable = false

        return FuncTyUnit
    }

    private fun FuncSpecialApplyExpression.inferType(
        expected: Expectation
    ): FuncTy {
        val expressions = expressionList
        val lhs = expressions.getOrNull(0)
        val rhs = expressions.getOrNull(1)
        lhs?.inferType()
        rhs?.inferType()
        return FuncTyUnit
    }

    private fun FuncReferenceExpression.inferType(
        expected: Expectation
    ): FuncTy {
        if (nextReferenceExpressionIsVariable) {
            currentScope.addLocalSymbol(this)
        } else {
            resolve()
        }
        return FuncTyUnknown
    }

    private fun FuncReferenceExpression.resolve() {
        val localSymbol = currentScope.lookupSymbol(this.text)
        if (localSymbol != null) {
            ctx.setResolvedRefs(this, listOf(PsiElementResolveResult(localSymbol)))
            return
        }

        val globalSymbols = globalLookup.resolve(this)
        if (globalSymbols != null) {
            ctx.setResolvedRefs(this, globalSymbols.map { PsiElementResolveResult(it) })
            return
        }
    }

    private fun FuncInvExpression.inferType(
        expected: Expectation
    ): FuncTy {
        val expression = expression
        val ty = expression?.inferType(expected)
        return ty ?: FuncTyUnknown
    }

    private fun FuncTernaryExpression.inferType(
        expected: Expectation
    ): FuncTy {
        condition.inferType(FuncTyInt)
        val thenTy = thenBranch?.inferType(expected)
        val elseTy = elseBranch?.inferType(expected)
        return thenTy ?: elseTy ?: FuncTyUnknown
    }

    private fun FuncUnaryMinusExpression.inferType(
        expected: Expectation
    ): FuncTy {
        val expression = expression
        val ty = expression?.inferType(expected)
        return ty ?: FuncTyUnknown
    }

    private fun List<FuncExpression>.inferType(
        expected: List<FuncTy>?
    ): List<FuncTy> {
        val extended = expected.orEmpty().asSequence().infiniteWith(FuncTyUnknown)
        return asSequence().zip(extended).map { (expr, ty) ->
            expr.inferTypeCoercibleTo(ty)
        }.toList()
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
