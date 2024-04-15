package org.ton.intellij.tact.type

import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.containers.OrderedSet
import org.ton.intellij.tact.diagnostics.TactDiagnostic
import org.ton.intellij.tact.psi.*
import org.ton.intellij.tact.psi.impl.ty
import org.ton.intellij.util.ancestorStrict
import org.ton.intellij.util.parentOfType

class TactTypeInferenceWalker(
    val ctx: TactInferenceContext,
    private val returnTy: TactTy
) {
    private val variables = HashMap<String, Pair<TactNamedElement, TactTy>>()

    fun walk(block: TactBlock) {
        block.inferType()
    }

    private fun addVariable(name: String, element: TactNamedElement, ty: TactTy) {
        variables[name] = element to ty
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
                val variableTy = type?.ty
                val expressionTy = expression?.inferType()
                if (variableTy != null && expressionTy != null && !expressionTy.isAssignable(variableTy)) {
                    ctx.reportTypeMismatch(this, variableTy, expressionTy)
                }
                val name = name
                val definedVariable = variables[name]
                if (name != null) {
                    if (definedVariable != null) {
                        ctx.addDiagnostic(TactDiagnostic.VariableAlreadyExists(this, definedVariable.first))
                    } else if (variableTy != null) {
                        addVariable(name, this, variableTy)
                    }
                }
            }
            is TactExpressionStatement -> expression.inferType()
            is TactReturnStatement -> expression?.inferType()
            is TactAssignStatement -> expressionList.forEach { it.inferType() }
            is TactWhileStatement -> {
                condition?.expression?.inferType()
                block?.inferType()
            }
            is TactConditionStatement -> {
                condition?.expression?.inferType()
                block?.inferType()
                elseBranch?.conditionStatement?.inferType()
                elseBranch?.block?.inferType()
            }
        }
    }

    private fun TactExpression.inferType(): TactTy? {
        ProgressManager.checkCanceled()
        var ty = ctx.getExprTy(this)
        if (ty != null) {
            return ty
        }
        ty = when (this) {
            is TactBinExpression -> inferType()
            is TactParenExpression -> inferType()
            is TactDotExpression -> inferType()
            is TactReferenceExpression -> inferType()
            is TactCallExpression -> inferType()
            is TactInitOfExpression -> inferType()
            is TactStructExpression -> inferType()
            is TactSelfExpression -> inferType()
            is TactUnaryExpression -> inferType()
            is TactNotNullExpression -> inferType()
            else -> null
        }
        if (ty != null) {
            ctx.setExprTy(this, ty)
        }
        return ty
    }

    private fun TactBinExpression.inferType(): TactTy {
        return expressionList.map {
            it.inferType()
        }.firstOrNull() ?: TactTyUnknown
    }

    private fun TactSelfExpression.inferType(): TactTy = getParentFunction()?.selfType ?: TactTyUnknown
    private fun TactParenExpression.inferType(): TactTy = expression?.inferType() ?: TactTyUnknown
    private fun TactUnaryExpression.inferType(): TactTy = expression?.inferType() ?: TactTyUnknown

    private fun TactInitOfExpression.inferType(): TactTy {
        val type = (reference?.resolve() as? TactTypeDeclarationElement)?.declaredTy
        expressionList.forEach {
            it.inferType()
        }
        return type ?: TactTyUnknown
    }

    private fun TactDotExpression.inferType(): TactTy? {
        val expressions = expressionList
        val left = expressions.getOrNull(0)
        val right = expressions.getOrNull(1)

        val leftType = left?.inferType()

        when (right) {
            is TactFieldExpression -> {
                if (leftType is TactTyRef) {
                    val name = right.identifier.text
                    val members = leftType.item.members
                    val resolvedField = members.find { it.name == name }
                    if (resolvedField != null) {
                        ctx.setResolvedRefs(right, OrderedSet(listOf(PsiElementResolveResult(resolvedField))))
                        return when (resolvedField) {
                            is TactField -> resolvedField.type?.ty
                            is TactConstant -> resolvedField.type?.ty
                            else -> null
                        }
                    }
                }
            }

            is TactCallExpression -> {
                var returnTy: TactTy? = null
                if (leftType is TactTyRef) {
                    val name = right.identifier.text
                    val members = leftType.item.members
                    val resolvedFunction = members.find { it.name == name } as? TactFunction
                    if (resolvedFunction != null) {
                        ctx.setResolvedRefs(right, OrderedSet(listOf(PsiElementResolveResult(resolvedFunction))))
                        returnTy = resolvedFunction.type?.ty
                    }
                }
                right.expressionList.forEach {
                    it.inferType()
                }
                return returnTy
            }
        }

        return null
    }

    private fun TactReferenceExpression.inferType(): TactTy {
        val referenceName = identifier.text
        val candidates = collectVariableCandidates(this).filter { it.name == referenceName }
        if (candidates.isNotEmpty()) {
            ctx.setResolvedRefs(this, OrderedSet(candidates.map { PsiElementResolveResult(it) }))
        }
        return when (val candidate = candidates.firstOrNull()) {
            is TactFunctionParameter -> {
                val resolved = candidate.type?.reference?.resolve()
                (resolved as? TactTypeDeclarationElement)?.declaredTy
            }
            is TactLetStatement -> candidate.type?.ty
            else -> null
        } ?: TactTyUnknown
    }

    private fun TactCallExpression.inferType(): TactTy {
        expressionList.forEach { it.inferType() }
        return TactTyUnknown
    }

    private fun TactStructExpression.inferType(): TactTy {
        val type = (reference?.resolve() as? TactTypeDeclarationElement)?.declaredTy
        structExpressionFieldList.forEach { field ->
            field.expression?.inferType()
        }
        return type ?: TactTyUnknown
    }

    private fun TactNotNullExpression.inferType(): TactTy? {
        val inferType = expression.inferType()
        return if (inferType is TactTyNullable) {
            inferType.inner
        } else {
            inferType
        }
    }
}

fun TactElement.getParentFunction(): TactInferenceContextOwner? {
    return ancestorStrict<TactInferenceContextOwner>()
}

val TactInferenceContextOwner.selfType: TactTy?
    get() = parentOfType<TactTypeDeclarationElement>()?.declaredTy ?: if (this is TactFunction) {
        functionParameters?.selfParameter?.let {
            it.type?.ty
        }
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

            is TactFunctionLike -> {
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
