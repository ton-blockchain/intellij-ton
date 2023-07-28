package org.ton.intellij.func.ide

import com.intellij.codeInsight.hints.InlayInfo
import com.intellij.codeInsight.hints.InlayParameterHintsProvider
import com.intellij.psi.PsiElement
import org.ton.intellij.func.psi.*

@Suppress("UnstableApiUsage")
class FuncParameterHintsProvider : InlayParameterHintsProvider {
    override fun getDefaultBlackList() = emptySet<String>()

    override fun getParameterHints(element: PsiElement): List<InlayInfo> {
        if (element !is FuncCallExpression) return emptyList()
        val expressionList = element.expressionList
        val function = expressionList.firstOrNull()?.reference?.resolve() as? FuncFunction ?: return emptyList()
        var arguments = expressionList.getOrNull(1)?.let {
            when (it) {
                is FuncTensorExpression -> it.expressionList
                is FuncReferenceExpression, is FuncTupleExpression -> listOf(it)
                else -> null
            }
        } ?: return emptyList()
        var parameters = function.functionParameterList
        if (element.isQualified) {
            val parent = element.parent
            if (parent is FuncQualifiedExpression) {
                arguments = listOf(parent.expressionList.first()) + arguments
            } else {
                if (parameters.isNotEmpty() && parameters.firstOrNull()?.atomicType is FuncTypeIdentifier) {
                    parameters.removeFirst()
                }
            }
        }
        val result = ArrayList<InlayInfo>()
        for ((index, funcExpression) in arguments.withIndex()) {
            if (index == 0 && element.isQualified) continue
            val parameter = parameters.getOrNull(index) ?: return result
            val parameterName = parameter.name
            if (parameterName != null) {
                result.add(InlayInfo(parameterName, funcExpression.textOffset))
            }
        }
        return result
    }
}
