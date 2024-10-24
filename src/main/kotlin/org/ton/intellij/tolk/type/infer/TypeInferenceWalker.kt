package org.ton.intellij.tolk.type.infer

import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiElementResolveResult
import org.ton.intellij.tolk.psi.*
import org.ton.intellij.tolk.type.ty.*
import org.ton.intellij.util.infiniteWith

class TolkTypeInferenceWalker(
    val ctx: TolkInferenceContext,
    private val returnTy: TolkTy
) {
    private var variableDeclarationState = false
    private val definedVariables = ArrayDeque<HashMap<String, MutableList<TolkNamedElement>>>()

    private fun define(vararg namedElements: TolkNamedElement) {
        definedVariables.firstOrNull()?.let {
            val name = namedElements.firstOrNull()?.name?.removeSurrounding("`") ?: return
            it.getOrPut(name) {
                ArrayList()
            }.addAll(namedElements)
        }
    }

    private fun resolve(name: String): List<TolkNamedElement>? {
        val formatted = name.removeSurrounding("`")
        definedVariables.forEach {
            val variable = it[formatted] ?: return@forEach
            return variable
        }
        return null
    }

    fun inferFunctionBody(
        block: TolkBlockStatement,
        functionParameterList: MutableList<TolkFunctionParameter>,
    ): TolkTy {
        definedVariables.addFirst(HashMap())
        functionParameterList.forEach { param ->
            define(param)
        }
        return block.inferTypeCoercibleTo(returnTy)
    }

    private fun TolkBlockStatement.inferTypeCoercibleTo(
        expected: TolkTy
    ): TolkTy {
        val type = inferType(Expectation.ExpectHasTy(expected), coerce = true)
        return type
    }

    private fun TolkBlockStatement.inferType(
        expected: Expectation = Expectation.NoExpectation,
        coerce: Boolean = false
    ): TolkTy {
        if (!isValid) return TolkTyUnknown
        definedVariables.addFirst(HashMap())
        val statements = statementList
        for (statement in statements) {
            statement.inferType()
        }
        definedVariables.removeFirst()

        return if (expected is Expectation.ExpectHasTy) {
            expected.ty
        } else {
            TolkTyUnknown
        }
    }

    private fun TolkStatement.inferType() {
        when (this) {
            is TolkReturnStatement -> inferType()
            is TolkBlockStatement -> inferType()
            is TolkRepeatStatement -> inferType()
            is TolkIfStatement -> inferType()
            is TolkDoStatement -> inferType()
            is TolkWhileStatement -> inferType()
            is TolkTryStatement -> inferType()
            is TolkVarStatement -> inferType()
            is TolkExpressionStatement -> expression.inferType()
            else -> {
            }
        }
    }

    private fun TolkExpression.inferTypeCoercibleTo(
        expected: TolkTy
    ): TolkTy {
        val inferred = inferType(expected.maybeHasType())
        // TODO: coerce to expected
        return inferred
    }

    private fun TolkExpression.inferType(expected: TolkTy?) =
        inferType(expected.maybeHasType())

    private fun TolkExpression.inferType(
        expected: Expectation = Expectation.NoExpectation,
    ): TolkTy {
        ProgressManager.checkCanceled()
        if (ctx.isTypeInferred(this)) {
            error("Type already inferred for $this")
        }
        val ty = when (this) {
            is TolkLiteralExpression -> inferLiteral(this)
            is TolkParenExpression -> inferType(expected)
            is TolkTensorExpression -> inferType(expected)
            is TolkTupleExpression -> inferType(expected)
            is TolkBinExpression -> inferType(expected)
//            is TolkApplyExpression -> inferType(expected) // TODO: fix
            is TolkSpecialApplyExpression -> inferType(expected)
            is TolkReferenceExpression -> inferType(expected)
            is TolkInvExpression -> inferType(expected)
            is TolkTernaryExpression -> inferType(expected)
            is TolkUnaryExpression -> inferType(expected)
            else -> TolkTyUnknown
        }?.getTolkTy() ?: TolkTyUnknown

        ctx.setExprTy(this, ty)

        return ty
    }

    private fun inferLiteral(expr: TolkLiteralExpression): TolkTy? {
        // TODO: optimize via stubs
        if (expr.integerLiteral != null || expr.trueKeyword != null || expr.falseKeyword != null) {
            return TolkTyInt
        } else if (expr.stringLiteral != null) {
            return TolkTySlice
        }
        return null
    }

    private fun TolkReturnStatement.inferType(): TolkTy {
        return expression?.inferTypeCoercibleTo(returnTy) ?: TolkTyUnknown
    }

    private fun TolkRepeatStatement.inferType(): TolkTy {
        expression?.inferType(TolkTyInt)
        blockStatement?.inferType()
        return TolkTyUnit
    }

    private fun TolkIfStatement.inferType(): TolkTy {
        condition?.inferType(TolkTyInt)
        blockStatement?.inferType()
        elseBranch?.statement?.inferType()
        return TolkTyUnit
    }

    private fun TolkDoStatement.inferType(): TolkTy {
        blockStatement?.inferType()
        condition?.inferType(TolkTyInt)
        return TolkTyUnit
    }

    private fun TolkWhileStatement.inferType(): TolkTy {
        condition?.inferType(TolkTyInt)
        blockStatement?.inferType()
        return TolkTyUnit
    }

//    private fun TolkTryStatement.inferType(): TolkTy {
//        blockStatement?.inferType()
//
//        definedVariables.addFirst(HashMap())
//        catch?.referenceExpressionList?.forEachIndexed { index, tolkReferenceExpression ->
//            if (index == 1) {
//                ctx.setExprTy(tolkReferenceExpression, TolkTyInt)
//            }
//            define(tolkReferenceExpression)
//        }
//        catch?.blockStatement?.inferType()
//        definedVariables.removeFirst()
//        return TolkTyUnit
//    }

    private fun TolkTensorExpression.inferType(
        expected: Expectation
    ): TolkTy {
//        val fields = expected.onlyHasTy(ctx)?.let {
//            (it as? TolkTyTensor)?.types
////            (resolveTypeVarsWithObligations(it) as? TolkTyTensor)?.types TODO implement resolveTypeVarsWithObligations
//        }
        return TolkTyTensor(expressionList.inferType(null))
    }

    private fun TolkTupleExpression.inferType(
        expected: Expectation
    ): TolkTy {
        return TolkTy(expressionList.inferType(null))
    }

    private fun TolkParenExpression.inferType(
        expected: Expectation
    ): TolkTy {
        return expression?.inferType(expected) ?: TolkTyUnknown
    }

    private fun TolkBinExpression.inferType(
        expected: Expectation
    ): TolkTy {
        val leftTy = left.inferType()
        val rightTy = right?.inferTypeCoercibleTo(leftTy)
        return rightTy ?: leftTy
    }

//    private fun TolkApplyExpression.inferType(
//        expected: Expectation
//    ): TolkTy {
//        val expressions = expressionList
//        val lhs = expressions[0]
//        val rhs = expressions.getOrNull(1)
//
//        fun TolkExpression.isTypeExpression(): Boolean {
//            return when (this) {
//                is TolkPrimitiveTypeExpression,
//                is TolkHoleTypeExpression -> true
//
//                is TolkTupleExpression -> expressionList.all { it.isTypeExpression() }
//                is TolkTensorExpression -> expressionList.all { it.isTypeExpression() }
//                else -> false
//            }
//        }
//
//        if (lhs.isTypeExpression()) {
//            variableDeclarationState = true
//        }
//        val lhsTy = lhs?.inferType()
//        val rhsTy = rhs?.inferType()
//        variableDeclarationState = false
//
//        return TolkTyUnit
//    }

    private fun TolkSpecialApplyExpression.inferType(
        expected: Expectation
    ): TolkTy {
        val expressions = expressionList
        val lhs = expressions.getOrNull(0)
        val rhs = expressions.getOrNull(1)
        val lhsTy = lhs?.inferType()
        val rhsTy = rhs?.inferType()
        return TolkTyUnit
    }

    private fun TolkReferenceExpression.inferType(
        expected: Expectation
    ): TolkTy {
        if (variableDeclarationState) {
            define(this)
        } else {
            resolve(this.name ?: return TolkTyUnknown)?.let { resolved ->
                ctx.setResolvedRefs(this, resolved.map { PsiElementResolveResult(it) })
            }
        }
        return TolkTyUnknown
    }

    private fun TolkInvExpression.inferType(
        expected: Expectation
    ): TolkTy {
        val expression = expression
        val ty = expression?.inferType(expected)
        return ty ?: TolkTyUnknown
    }


    private fun TolkTernaryExpression.inferType(
        expected: Expectation
    ): TolkTy {
        val conditionTy = condition.inferType(TolkTyInt)
        val thenTy = thenBranch?.inferType(expected)
        val elseTy = elseBranch?.inferType(expected)
        return thenTy ?: elseTy ?: TolkTyUnknown
    }

    private fun TolkUnaryExpression.inferType(
        expected: Expectation
    ): TolkTy {
        val expression = expression
        val ty = expression?.inferType(expected)
        return ty ?: TolkTyUnknown
    }

    private fun List<TolkExpression>.inferType(
        expected: List<TolkTy>?
    ): List<TolkTy> {
        val extended = expected.orEmpty().asSequence().infiniteWith(TolkTyUnknown)
        return asSequence().zip(extended).map { (expr, ty) ->
            expr.inferTypeCoercibleTo(ty)
        }.toList()
    }
}
