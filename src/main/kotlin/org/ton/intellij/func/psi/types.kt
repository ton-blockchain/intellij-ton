package org.ton.intellij.func.psi

import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import org.ton.intellij.childOfType
import org.ton.intellij.parentOfType

sealed interface FuncTypeName

object FuncIntTypeName : FuncTypeName {
    override fun toString(): String = "int"
}

object FuncCellTypeName : FuncTypeName {
    override fun toString(): String = "cell"
}

object FuncSliceTypeName : FuncTypeName {
    override fun toString(): String = "slice"
}

object FuncBuilderTypeName : FuncTypeName {
    override fun toString(): String = "builder"
}

object FuncContTypeName : FuncTypeName {
    override fun toString(): String = "cont"
}

data class FuncTensorTypeName(val list: List<FuncTypeName?> = emptyList()) : FuncTypeName, List<FuncTypeName?> by list {
    override fun toString() = list.joinToString(prefix = "(", postfix = ")")
}

private fun PsiElement.resolveType() = when {
    elementType == FuncTokenTypes.INTEGER_LITERAL -> FuncIntTypeName
    elementType == FuncTokenTypes.STRING_LITERAL -> when (text.last()) {
        'u', 'h', 'H', 'c' -> FuncIntTypeName
        else -> FuncSliceTypeName
    }
    this is FuncElement -> resolveType()
    else -> null
}

fun FuncElement.resolveType(): FuncTypeName? {
    val resolved = when {
        this is FuncExpression -> firstChild.resolveType()
        this is FuncAssignmentExpression -> assignmentExpression?.resolveType() ?: ternaryExpression.resolveType()
        this is FuncTernaryExpression -> ternaryExpression?.resolveType() ?: equationExpression.resolveType()
        this is FuncEquationExpression -> bitwiseExpressionList.firstOrNull()?.resolveType()
        this is FuncBitwiseExpression -> arithmeticExpressionList.firstOrNull()?.resolveType()
        this is FuncArithmeticExpression -> expr30List.firstOrNull()?.resolveType()
        this is FuncExpr30 -> expr75List.firstOrNull()?.resolveType()
        this is FuncExpr75 -> expr80.resolveType()
        this is FuncExpr80 -> {
            val type = expr90.resolveType()
            if (callableList.isNotEmpty()) {
                var currentChainType: FuncTypeName? = type
                callableList.forEach { callable ->
                    currentChainType = callable.resolveType()
                }
                currentChainType
            } else type
        }
        this is FuncExpr90 -> firstChild.resolveType()
        this is FuncExpr100 -> firstChild.resolveType()
        this is FuncTypeExpression -> firstChild.resolveType()
        this is FuncFunctionCall -> {
            val funcFunction = reference?.resolve() as? FuncFunction
            val parameters = tensorExpression.resolveType()?.list
            funcFunction?.resolveTemplate(parameters)
        }
        this is FuncCallable -> firstChild.resolveType()
        this is FuncMethodCall -> reference?.resolve()?.resolveType()
        this is FuncModifyingMethodCall -> resolveType()
        this is FuncFunction -> resolveType()
        this is FuncNonTypeExpression -> firstChild.resolveType()
        this is FuncTensorType -> FuncTensorTypeName(tensorTypeItemList.map {
            it.firstChild.resolveType()
        })
        this is FuncTensorExpression -> resolveType()
        this is FuncFunctionReturnType -> firstChild.resolveType()
        this is FuncPrimitiveType -> resolveType()
        this is FuncParameterDeclaration -> firstChild.resolveType()
        this is FuncReferenceExpression -> {
            val resolved = reference?.resolve()
            if (resolved is FuncReferenceExpression) {
                null
            } else {
                resolved?.resolveType()
            }
        }
        this is FuncVariableDeclaration -> typeExpression.resolveType()
        else -> null
    }
    return resolved
}

fun FuncFunction.resolveType(): FuncTypeName? {
    return if (functionReturnType.holeType != null) {
        return blockStatement?.childOfType<FuncReturnStatement>()?.expression?.resolveType()
    } else {
        functionReturnType.resolveType()
    }
}

fun FuncModifyingMethodCall.resolveType(): FuncTypeName? {
    val funcFunction = reference?.resolve() as? FuncFunction ?: return null
    val functionType = funcFunction.resolveType() ?: return null
    if (functionType is FuncTensorTypeName) {
        if (functionType.size >= 2) {
            return functionType[1]
        }
    }
    return functionType
}

fun FuncFunction.resolveTemplate(parameters: List<FuncTypeName?>?): FuncTypeName? {
    val typeIdentifierName = functionReturnType.typeIdentifier?.identifier?.text ?: return resolveType()
    parameterList.parameterDeclarationList.forEachIndexed { index, funcParameterDeclaration ->
        val parameterTypeIdentifierName =
            funcParameterDeclaration.typeIdentifier?.identifier?.text ?: return@forEachIndexed
        if (parameterTypeIdentifierName == typeIdentifierName) {
            return if (parameters != null) {
                parameters[index]
            } else {
                resolveType()
            }
        }
    }
    return resolveType()
}

fun FuncPrimitiveType.resolveType() = when (firstChild.elementType) {
    FuncTokenTypes.INT -> FuncIntTypeName
    FuncTokenTypes.CELL -> FuncCellTypeName
    FuncTokenTypes.SLICE -> FuncSliceTypeName
    FuncTokenTypes.BUILDER -> FuncBuilderTypeName
    FuncTokenTypes.CONT -> FuncContTypeName
    else -> null
}

fun FuncTensorExpression.resolveType(): FuncTensorTypeName? {
    var resolved: FuncTensorTypeName? = null
    if (parent is FuncVariableDeclaration) {
        parent.parentOfType<FuncAssignmentExpression>()?.resolveType()
    } else {
        resolved = FuncTensorTypeName(tensorExpressionItemList.map {
            it.expression.resolveType()
        })
    }
    return resolved
}
