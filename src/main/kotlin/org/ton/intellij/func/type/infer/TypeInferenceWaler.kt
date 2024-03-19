package org.ton.intellij.func.type.infer

import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiElementResolveResult
import org.ton.intellij.func.psi.*
import org.ton.intellij.func.type.ty.*
import org.ton.intellij.util.infiniteWith

class FuncTypeInferenceWalker(
    val ctx: FuncInferenceContext,
    private val returnTy: FuncTy
) {
    private val definitions = HashMap<String, FuncElement>()
    private var variableDeclarationState = false

    fun inferFunctionBody(
        block: FuncBlockStatement
    ): FuncTy = block.inferTypeCoercibleTo(returnTy)

    private fun FuncBlockStatement.inferTypeCoercibleTo(
        expected: FuncTy
    ): FuncTy = inferType(Expectation.ExpectHasTy(expected), coerce = true)

    private fun FuncBlockStatement.inferType(
        expected: Expectation = Expectation.NoExpectation,
        coerce: Boolean = false
    ): FuncTy {
        for (statement in statementList) {
            statement.inferType()
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
            else -> {
            }
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
            else -> FuncTyUnknown
        }

        ctx.setExprTy(this, ty)

        return ty
    }

    private fun inferLiteral(expr: FuncLiteralExpression): FuncTy {
        // TODO: optimize via stubs
        if (expr.integerLiteral != null || expr.trueKeyword != null || expr.falseKeyword != null) {
            return FuncTyInt
        } else if (expr.stringLiteral != null) {
            return FuncTySlice
        }
        return FuncTyUnknown
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
        blockStatement?.inferType()
        condition?.inferType(FuncTyInt)
        return FuncTyUnit
    }

    private fun FuncWhileStatement.inferType(): FuncTy {
        condition?.inferType(FuncTyInt)
        blockStatement?.inferType()
        return FuncTyUnit
    }

    private fun FuncTryStatement.inferType(): FuncTy {
        blockStatement?.inferType()
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

        if (lhs is FuncPrimitiveTypeExpression || lhs is FuncHoleTypeExpression) {
            variableDeclarationState = true
        }
        val lhsTy = lhs?.inferType()
        val rhsTy = rhs?.inferType()
        variableDeclarationState = false

        return FuncTyUnit
    }

    private fun FuncSpecialApplyExpression.inferType(
        expected: Expectation
    ): FuncTy {
        val expressions = expressionList
        val lhs = expressions.getOrNull(0)
        val rhs = expressions.getOrNull(1)
        val lhsTy = lhs?.inferType()
        val rhsTy = rhs?.inferType()
        return FuncTyUnit
    }

    private fun FuncReferenceExpression.inferType(
        expected: Expectation
    ): FuncTy {
        if (variableDeclarationState) {
            ctx.lookup.define(this)
        } else {
            ctx.lookup.resolve(this)?.let { resolved ->
                ctx.setResolvedRefs(this, resolved.map { PsiElementResolveResult(it) })
            }
        }
        return FuncTyUnknown
    }

    private fun FuncInvExpression.inferType(
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
