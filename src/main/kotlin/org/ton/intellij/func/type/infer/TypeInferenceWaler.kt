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
            processStatement(statement)
        }

        return if (expected is Expectation.ExpectHasTy) {
            expected.ty
        } else {
            FuncTyUnknown
        }
    }

    private fun processStatement(psi: FuncStatement) {
        when (psi) {
            is FuncReturnStatement -> psi.inferType()
            is FuncBlockStatement -> psi.inferType()
            is FuncRepeatStatement -> psi.inferType()
            is FuncIfStatement -> psi.inferType()
            is FuncDoStatement -> psi.inferType()
            is FuncWhileStatement -> psi.inferType()
            is FuncTryStatement -> psi.inferType()
            is FuncExpressionStatement -> psi.expression.inferType()
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
            is FuncTensorExpression -> inferType(expected)
            is FuncAssignExpression -> inferType(expected)
            is FuncApplyExpression -> inferType(expected)
            is FuncSpecialApplyExpression -> inferType(expected)
            is FuncReferenceExpression -> inferType(expected)
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
        condition?.expression?.inferType(FuncTyInt)
        blockStatement?.inferType()
        elseBranch?.blockStatement?.inferType()

        var elseIfBranch = elseIfBranch
        while (elseIfBranch != null) {
            elseIfBranch.condition?.expression?.inferType(FuncTyInt)
            elseIfBranch.blockStatement?.inferType()
            elseIfBranch.elseBranch?.blockStatement?.inferType()
            elseIfBranch = elseIfBranch.elseIfBranch
        }

        return FuncTyUnit
    }

    private fun FuncDoStatement.inferType(): FuncTy {
        blockStatement?.inferType()
        condition?.expression?.inferType(FuncTyInt)
        return FuncTyUnit
    }

    private fun FuncWhileStatement.inferType(): FuncTy {
        condition?.expression?.inferType(FuncTyInt)
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

    private fun FuncAssignExpression.inferType(
        expected: Expectation
    ): FuncTy {
        val expressions = expressionList
        val lhs = expressions.getOrNull(0)
        val rhs = expressions.getOrNull(1)
        val lhsTy = lhs?.inferType()
        val rhsTy = rhs?.inferType()
//        val rhsTy = rhs?.inferTypeCoercibleTo(lhsTy)
//        return lhsTy
        return FuncTyUnit
    }

    private fun FuncApplyExpression.inferType(
        expected: Expectation
    ): FuncTy {
        val expressions = expressionList
        val lhs = expressions.getOrNull(0)
        val rhs = expressions.getOrNull(1)

        val lhsTy = lhs?.inferType()
        if (lhs is FuncHoleTypeExpression) {
            variableDeclarationState = true
        }
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
        println("try infer type for ${this.text} in ${this.parent.text} - state = $variableDeclarationState")
        if (variableDeclarationState) {
            ctx.lookup.define(this)
        } else {
            ctx.lookup.resolve(this)?.let {
                ctx.setResolvedRefs(this, listOf(PsiElementResolveResult(it)))
            }
        }
        return FuncTyUnknown
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
