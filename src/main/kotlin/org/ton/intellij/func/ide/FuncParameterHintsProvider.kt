package org.ton.intellij.func.ide

import com.intellij.codeInsight.hints.InlayInfo
import com.intellij.codeInsight.hints.InlayParameterHintsProvider
import com.intellij.psi.PsiElement
import org.ton.intellij.func.psi.*

@Suppress("UnstableApiUsage")
class FuncParameterHintsProvider : InlayParameterHintsProvider {
    override fun getDefaultBlackList() = emptySet<String>()

    override fun getParameterHints(element: PsiElement): List<InlayInfo> {
        val function: FuncFunction
        val argumentOffset: Int
        val arguments: List<FuncExpression>
        when (element) {
            is FuncCallExpression -> {
                function = element.referenceExpression.resolveFunction() ?: return emptyList()
                argumentOffset = 0
                arguments = element.callArgument.toArgList()
            }

            is FuncMethodCall -> {
                function = element.referenceExpression.resolveFunction() ?: return emptyList()
                argumentOffset = 1
                arguments = element.callArgument?.toArgList() ?: return emptyList()
            }

            else -> return emptyList()
        }
        val parameters = function.functionParameterList
        val result = ArrayList<InlayInfo>()
        arguments.forEachIndexed { index, funcExpression ->
            val parameter = parameters.getOrNull(index + argumentOffset) ?: return result
            val parameterName = parameter.name
            if (parameterName != null) {
                result.add(InlayInfo(parameterName, funcExpression.textOffset))
            }
        }
        return result
    }

    private fun FuncReferenceExpression.resolveFunction() = reference?.resolve() as? FuncFunction
    private fun FuncCallArgument.toArgList() = expression.toArgList()
    private fun FuncExpression.toArgList() = if (this is FuncTensorExpression) {
        this.expressionList
    } else {
        listOf(this)
    }
}
