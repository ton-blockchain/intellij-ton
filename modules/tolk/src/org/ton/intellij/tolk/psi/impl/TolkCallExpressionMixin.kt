package org.ton.intellij.tolk.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import org.ton.intellij.tolk.psi.TolkCallExpression
import org.ton.intellij.tolk.psi.TolkDotExpression
import org.ton.intellij.tolk.psi.TolkExpression
import org.ton.intellij.tolk.type.TolkType
import org.ton.intellij.tolk.type.inference

val TolkCallExpression.actualArgumentList: List<TolkExpression>
    get() {
        val dotExpr = parent as? TolkDotExpression
        val firstArg = dotExpr?.left
        val actualArgumentList = ArrayList<TolkExpression>()
        if (firstArg != null && firstArg != this) {
            actualArgumentList.add(firstArg)
        }
        argumentList.argumentList.forEach { arg ->
            val argument = arg.expression
            actualArgumentList.add(argument)
        }
        return actualArgumentList
    }

abstract class TolkCallExpressionMixin(node: ASTNode) : ASTWrapperPsiElement(node), TolkCallExpression {
    override val type: TolkType? get() = inference?.getType(this)

//    fun resolveCall(
//        expectedReturnType: TolkType? = null
//    ) : TolkType? {
//        println("start resolving `${text}` with expected $expectedReturnType")
//        val functionType = expression.type as? TolkType.Function ?: return null
//        val returnType = functionType.returnType
//        val parameterTypes = when(val parameter = functionType.inputType) {
//            is TolkType.TolkTupleType -> parameter.elements
//            TolkType.Unit -> emptyList()
//            else -> listOf(parameter)
//        }
//
//        val dotExpr = parent as? TolkDotExpression
//        val firstArg = dotExpr?.left
//        val argumentTypes = ArrayList<TolkType>()
//        var argIndex = 0
//        if (firstArg != null && firstArg != this) {
//            val argumentType = firstArg.type
//            if (argumentType == null) {
//                println("arg 0 is null")
//                return null
//            }
//            println("arg 0 = $argumentType")
//            argumentTypes.add(argumentType)
//            argIndex++
//        }
//        actualArgumentList.forEach { arg ->
//            val argument = arg
//            val argumentType = when (argument) {
//                is TolkDotExpression -> {
//                    (argument.right as? TolkCallExpressionMixin)?.resolveCall(parameterTypes.getOrNull(argIndex))
//                }
//                is TolkCallExpressionMixin -> {
//                    argument.resolveCall(parameterTypes.getOrNull(argIndex))
//                }
//                else -> argument.type
//            }
//            if (argumentType == null) {
//                println("arg $argIndex is null")
//                return null
//            }
//            argumentTypes.add(argumentType)
//            println("arg $argIndex = $argumentType")
//
//            argIndex++
//        }
//        if (argumentTypes.size != parameterTypes.size) {
//            println("args(${argumentTypes.size}) != params(${parameterTypes.size})")
//            return null
//        }
//        val typeMapping = mutableMapOf<String, TolkType>()
//        parameterTypes.zip(argumentTypes).forEach { (param, arg) ->
//            if (param is TolkType.ParameterType && arg !is TolkType.ParameterType) {
//                typeMapping[param.name] = arg
//            }
//        }
//        return when(returnType) {
//            is TolkType.ParameterType -> typeMapping[returnType.name] ?: expectedReturnType ?: returnType
//            else -> returnType
//        }
//    }
}
