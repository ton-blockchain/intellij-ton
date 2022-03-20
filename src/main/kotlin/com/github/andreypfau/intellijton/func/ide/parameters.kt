package com.github.andreypfau.intellijton.func.ide

import com.github.andreypfau.intellijton.func.psi.FuncFunctionCall
import com.github.andreypfau.intellijton.func.psi.FuncMethodCall
import com.github.andreypfau.intellijton.func.psi.FuncModifyingMethodCall
import com.github.andreypfau.intellijton.func.resolve.resolveFunction
import com.intellij.codeInsight.hints.InlayInfo
import com.intellij.codeInsight.hints.InlayParameterHintsProvider
import com.intellij.psi.PsiElement

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
        val arguments = when (element) {
            is FuncFunctionCall -> element.tensorExpression
            is FuncMethodCall -> element.tensorExpression
            is FuncModifyingMethodCall -> element.tensorExpression
            else -> null
        }?.tensorExpressionItemList ?: return emptyList()
        val paramOffset = if (element is FuncFunctionCall) 0 else 1
        val paramNames = funcFunction.parameterList.parameterDeclarationList.map { it.identifier.text }
        return arguments.mapIndexed { index, expressionItem ->
            InlayInfo(paramNames[index + paramOffset], expressionItem.textOffset)
        }
    }
}