package org.ton.intellij.func.ide

import com.intellij.codeInsight.hints.InlayInfo
import com.intellij.codeInsight.hints.InlayParameterHintsProvider
import com.intellij.psi.PsiElement
import org.ton.intellij.childOfType
import org.ton.intellij.func.psi.FuncFunctionCall
import org.ton.intellij.func.psi.FuncMethodCall
import org.ton.intellij.func.psi.FuncModifyingMethodCall
import org.ton.intellij.func.psi.FuncReferenceExpression
import org.ton.intellij.func.resolve.resolveFunction

@Suppress("UnstableApiUsage")
class FuncParameterHintsProvider : InlayParameterHintsProvider {
    override fun getDefaultBlackList(): Set<String> = emptySet()
    override fun getParameterHints(element: PsiElement): List<InlayInfo> {
//        if (element is FuncExpression) {
//            return listOf(InlayInfo(element.resolveType().toString(), element.textOffset))
//        }
        val funcFunction = when (element) {
            is FuncFunctionCall -> element.resolveFunction()
            is FuncMethodCall -> element.resolveFunction()
            is FuncModifyingMethodCall -> element.resolveFunction()
            else -> null
        } ?: return emptyList()
        val parameterList = funcFunction.parameterList ?: return emptyList()
        val arguments = when (element) {
            is FuncFunctionCall -> element.tensorExpression
            is FuncMethodCall -> element.tensorExpression
            is FuncModifyingMethodCall -> element.tensorExpression
            else -> null
        }?.tensorExpressionItemList ?: return emptyList()
        val paramOffset = when (element) {
            is FuncFunctionCall -> 0
            else -> 1
        }
        val paramNames = parameterList.parameterDeclarationList.map { it.identifier.text }
        return arguments.mapIndexed { index, expressionItem ->
            if (expressionItem.expression.childOfType<FuncReferenceExpression>() != null) {
                null
            } else {
                val paramNameIndex = index + paramOffset
                if (paramNameIndex > paramNames.lastIndex) {
                    null
                } else {
                    InlayInfo(paramNames[index + paramOffset], expressionItem.textOffset)
                }
            }
        }.filterNotNull()
    }
}