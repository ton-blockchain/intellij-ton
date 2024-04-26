package org.ton.intellij.tact.type

import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiElementResolveResult
import com.intellij.util.containers.OrderedSet
import org.ton.intellij.tact.diagnostics.TactDiagnostic
import org.ton.intellij.tact.psi.*
import org.ton.intellij.tact.psi.impl.isGet
import org.ton.intellij.tact.psi.impl.ty
import org.ton.intellij.tact.stub.index.TactFunctionIndex
import org.ton.intellij.tact.stub.index.TactTypesIndex
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

            is TactAssignStatement -> {
                val expressions = expressionList
                val lValue = expressions.getOrNull(0)
                val rValue = expressions.getOrNull(1)

                val lValueTy = lValue?.inferType()
                val rValueTy = rValue?.inferType()
                if (lValueTy != null && rValueTy != null && !lValueTy.isAssignable(rValueTy)) {
                    ctx.reportTypeMismatch(this, lValueTy, rValueTy)
                }
            }

            is TactExpressionStatement -> expression.inferType()
            is TactConditionStatement -> {
                val conditionTy = condition?.expression?.inferType()
                if (conditionTy != null && (conditionTy !is TactTyRef || conditionTy.item.name != "Bool")) {
                    ctx.reportTypeMismatch(this, object : TactTy {
                        override fun toString(): String = "Bool"
                        override fun isAssignable(other: TactTy): Boolean = false
                    }, conditionTy)
                }
                block?.inferType()
                elseBranch?.conditionStatement?.inferType()
                elseBranch?.block?.inferType()
            }

            is TactReturnStatement -> expression?.inferType()
            is TactWhileStatement -> {
                val conditionTy = condition?.expression?.inferType()
                if (conditionTy != null && (conditionTy !is TactTyRef || conditionTy.item.name != "Bool")) {
                    ctx.reportTypeMismatch(this, object : TactTy {
                        override fun toString(): String = "Bool"
                        override fun isAssignable(other: TactTy): Boolean = false
                    }, conditionTy)
                }
                block?.inferType()
            }

            is TactUntilStatement -> {
                block?.inferType()
                val conditionTy = condition?.expression?.inferType()
                if (conditionTy != null && (conditionTy !is TactTyRef || conditionTy.item.name != "Bool")) {
                    ctx.reportTypeMismatch(this, object : TactTy {
                        override fun toString(): String = "Bool"
                        override fun isAssignable(other: TactTy): Boolean = false
                    }, conditionTy)
                }
            }

            is TactRepeatStatement -> {
                val conditionTy = condition?.expression?.inferType()
                if (conditionTy != null && (conditionTy !is TactTyRef || conditionTy.item.name != "Int")) {
                    ctx.reportTypeMismatch(this, object : TactTy {
                        override fun toString(): String = "Int"
                        override fun isAssignable(other: TactTy): Boolean = false
                    }, conditionTy)
                }
                block?.inferType()
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
            is TactTernaryExpression -> inferType()
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
            is TactStringExpression -> inferType()
            is TactIntegerExpression -> inferType()
            is TactBooleanExpression -> inferType()
            else -> null
        }
        if (ty != null) {
            ctx.setExprTy(this, ty)
        }
        return ty
    }

    private fun TactTernaryExpression.inferType(): TactTy? {
        val conditionTy = condition.inferType()
        val thenTy = thenBranch?.inferType()
        val elseTy = elseBranch?.inferType()
        if (conditionTy != null && (conditionTy !is TactTyRef || conditionTy.item.name != "Bool")) {
            ctx.reportTypeMismatch(this, object : TactTy {
                override fun toString(): String = "Bool"
                override fun isAssignable(other: TactTy): Boolean = false
            }, conditionTy)
        }
        if (thenTy != null && elseTy != null) {
            if (thenTy.isAssignable(elseTy)) {
                return thenTy
            } else if (elseTy.isAssignable(thenTy)) {
                return elseTy
            } else {
                ctx.reportTypeMismatch(this, thenTy, elseTy)
            }
        }
        return null
    }

    private fun TactBinExpression.inferType(): TactTy? {
        return expressionList.map {
            it.inferType()
        }.firstOrNull()
    }

    private fun TactSelfExpression.inferType(): TactTy? = getParentFunction()?.selfType
    private fun TactParenExpression.inferType(): TactTy? = expression?.inferType()
    private fun TactUnaryExpression.inferType(): TactTy? = expression?.inferType()

    private fun TactInitOfExpression.inferType(): TactTy? {
        expressionList.forEach {
            it.inferType()
        }
        val stateInit = TactTypesIndex.findElementsByName(project, "StateInit")
            .filterIsInstance<TactStruct>()
            .firstOrNull {
                it.containingFile.name == "contract.tact"
            }
        return stateInit?.declaredTy
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

                    val function = TactFunctionIndex.findElementsByName(project, name).find {
                        it.selfType?.isAssignable(leftType) == true
                    }

                    if (function != null) {
                        ctx.setResolvedRefs(right, OrderedSet(listOf(PsiElementResolveResult(function))))
                        returnTy = function.type?.ty
                    } else {
                        val members = leftType.item.members.toList()
                        val resolvedFunction = members.find { it.name == name } as? TactFunction
                        if (resolvedFunction != null) {
                            ctx.setResolvedRefs(right, OrderedSet(listOf(PsiElementResolveResult(resolvedFunction))))
                            returnTy = resolvedFunction.type?.ty
                        }
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

    private fun TactReferenceExpression.inferType(): TactTy? {
        val referenceName = identifier.text
        val candidates = ctx.collectVariableCandidates(this).filter { it.name == referenceName }
        if (candidates.isNotEmpty()) {
            ctx.setResolvedRefs(this, OrderedSet(listOf(PsiElementResolveResult(candidates.first()))))
        }
        return when (val candidate = candidates.firstOrNull()) {
            is TactFunctionParameter -> candidate.type?.ty
            is TactLetStatement -> candidate.type?.ty
            else -> null
        }
    }

    private fun TactCallExpression.inferType(): TactTy? {
        expressionList.forEach { it.inferType() }

        val name = identifier.text
        var candidates = TactFunctionIndex.findElementsByName(project, name)
        if (candidates.isNotEmpty()) {
            if (candidates.size > 1) {
                val newCandidates = candidates.filter { !it.isGet }
                if (newCandidates.isNotEmpty()) {
                    candidates = newCandidates
                }
            }

            ctx.setResolvedRefs(this, OrderedSet(candidates.map { PsiElementResolveResult(it) }))
        }

        var ty: TactTy? = null
        for (candidate in candidates) {
            ty = candidate.type?.ty
            if (ty != null) {
                break
            }
        }

        return ty
    }

    private fun TactStructExpression.inferType(): TactTy? {
        val type = (reference?.resolve() as? TactTypeDeclarationElement)?.declaredTy
        structExpressionFieldList.forEach { field ->
            field.expression?.inferType()
        }
        return type
    }

    private fun TactNotNullExpression.inferType(): TactTy? {
        val inferType = expression.inferType()
        return if (inferType is TactTyNullable) {
            inferType.inner
        } else {
            inferType
        }
    }

    private fun TactStringExpression.inferType(): TactTy? {
        return TactTyRef(TactTypesIndex.findElementsByName(project, "String").firstOrNull() ?: return null)
    }

    private fun TactIntegerExpression.inferType(): TactTy? {
        return TactTyRef(TactTypesIndex.findElementsByName(project, "Int").firstOrNull() ?: return null)
    }

    private fun TactBooleanExpression.inferType(): TactTy? {
        return TactTyRef(TactTypesIndex.findElementsByName(project, "Bool").firstOrNull() ?: return null)
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
