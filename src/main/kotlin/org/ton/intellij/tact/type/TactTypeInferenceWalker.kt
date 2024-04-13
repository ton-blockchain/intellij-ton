package org.ton.intellij.tact.type

import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.containers.OrderedSet
import org.ton.intellij.tact.psi.*
import org.ton.intellij.util.ancestorStrict
import org.ton.intellij.util.parentOfType

class TactTypeInferenceWalker(
    val ctx: TactInferenceContext,
    private val returnTy: TactTy
) {
    fun walk(block: TactBlock) {
        block.inferType()
    }

    private fun TactBlock.inferType() {
        val statements = statementList
        for (statement in statements) {
            statement.inferType()
        }
    }

    private fun TactStatement.inferType() {
        when (this) {
            is TactLetStatement -> {
                val expression = expression
                expression?.inferType()
            }

            is TactExpressionStatement -> expression.inferType()
            is TactReturnStatement -> expression?.inferType()
            is TactAssignStatement -> expressionList.forEach { it.inferType() }
        }
    }

    private fun TactExpression.inferType(): TactTy {
        ProgressManager.checkCanceled()
        check(!ctx.isTypeInferred(this)) {
            "Type already inferred for $this"
        }

        val ty = when (this) {
            is TactBinExpression -> inferType()
            is TactParenExpression -> inferType()
            is TactDotExpression -> inferType()
            is TactReferenceExpression -> inferType()
            is TactCallExpression -> inferType()
            else -> TactTyUnknown
        }
        ctx.setExprTy(this, ty)
        return ty
    }

    private fun TactBinExpression.inferType(): TactTy {
        return expressionList.map {
            it.inferType()
        }.firstOrNull() ?: TactTyUnknown
    }

    private fun TactParenExpression.inferType(): TactTy = expression?.inferType() ?: TactTyUnknown

    private fun TactDotExpression.inferType(): TactTy {
        val expressions = expressionList
        val left = expressions.getOrNull(0)
        val right = expressions.getOrNull(1)

        val leftType = if (left is TactSelfExpression) {
            left.getParentFunction()?.selfType
        } else {
            left?.inferType()
        } ?: return TactTyUnknown

        if (right is TactFieldExpression) {
            if (leftType is TactTyAdt) {
                val typeItem = leftType.item
                val fields = when (typeItem) {
                    is TactStruct -> typeItem.blockFields?.fieldList
                    is TactContract -> typeItem.contractBody?.fieldList
                    is TactMessage -> typeItem.blockFields?.fieldList
                    is TactTrait -> typeItem.traitBody?.fieldList
                    else -> null
                }
                val resolvedField = fields?.find { it.name == right.identifier.text }
                if (resolvedField != null) {
                    ctx.setResolvedRefs(right, OrderedSet(listOf(PsiElementResolveResult(resolvedField))))
                    (resolvedField.type?.reference?.resolve() as? TactTypeDeclarationElement)?.let {
                        return it.declaredType
                    }
                }
            }
        }

        return TactTyUnknown
    }

    private fun TactReferenceExpression.inferType(): TactTy {
        val referenceName = identifier.text
        val candidates = collectVariableCandidates(this).filter { it.name == referenceName }
        if (candidates.isNotEmpty()) {
            ctx.setResolvedRefs(this, OrderedSet(candidates.map { PsiElementResolveResult(it) }))
        }
        return when (val candidate = candidates.firstOrNull()) {
            is TactFunctionParameter -> (candidate.type?.reference?.resolve() as? TactTypeDeclarationElement)?.declaredType
            is TactLetStatement -> (candidate.type?.reference?.resolve() as? TactTypeDeclarationElement)?.declaredType
            else -> null
        } ?: TactTyUnknown
    }

    private fun TactCallExpression.inferType(): TactTy {
        expressionList.forEach { it.inferType() }
        return TactTyUnknown
    }
}

fun TactElement.getParentFunction(): TactInferenceContextOwner? {
    return ancestorStrict<TactInferenceContextOwner>()
}

val TactInferenceContextOwner.selfType: TactTy?
    get() = parentOfType<TactTypeDeclarationElement>()?.declaredType ?: if (this is TactFunction) {
        functionParameters?.selfParameter?.let {
            it.type?.reference?.resolve() as? TactTypeDeclarationElement
        }?.declaredType
    } else null

private fun collectVariableCandidates(element: TactReferenceExpression): Collection<TactNamedElement> {
    val variableCandidates = mutableListOf<TactNamedElement>()
    PsiTreeUtil.treeWalkUp(element, null) { scope, prevParent ->
        when (scope) {
            is TactBlock -> {
                scope.statementList.forEach { stmt ->
                    if (stmt == prevParent) return@forEach
                    when (stmt) {
                        is TactLetStatement -> {
                            variableCandidates.add(stmt)
                        }
                    }
                }
            }

            is TactFunction -> {
                scope.functionParameters?.functionParameterList?.forEach { param ->
                    variableCandidates.add(param)
                }
                return@treeWalkUp false
            }
        }
        true
    }
    return variableCandidates
}
