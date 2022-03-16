package com.github.andreypfau.intellijton.func.ide

import com.github.andreypfau.intellijton.func.psi.FuncFunctionCall
import com.github.andreypfau.intellijton.func.resolve.resolveFunction
import com.intellij.codeInsight.hints.InlayInfo
import com.intellij.codeInsight.hints.InlayParameterHintsProvider
import com.intellij.codeInsight.hints.Option
import com.intellij.psi.PsiElement

@Suppress("UnstableApiUsage")
class FuncParameterHintsProvider : InlayParameterHintsProvider {
    val smartOption: Option = Option("SMART_HINTS", { "settings.func.inlay.parameter.hints.only.smart" }, true)

//    override fun getSupportedOptions() = listOf(smartOption)

    override fun getDefaultBlackList(): Set<String> = emptySet()

    override fun getParameterHints(element: PsiElement): List<InlayInfo> {
        if (element is FuncFunctionCall) {
            val funcFunction = element.resolveFunction() ?: return emptyList()
            val paramNames = funcFunction.parameterList.parameterDeclarationList.map { it.identifier.text }
            return element.tensorExpression?.tensorExpressionItemList?.mapIndexed { index, expressionItem ->
                InlayInfo(paramNames[index], expressionItem.textOffset)
            } ?: emptyList()
        }
        return emptyList()
    }

//    override fun getHintInfo(element: PsiElement): HintInfo? {
//        val funcFunctionCall = element as? FuncFunctionCall ?: return null
//        val funcFunction = funcFunctionCall.functionCallIdentifier.reference?.resolve() as? FuncFunction ?: return null
//        val funcFunctionName = funcFunction.name ?: return null
//        val paramNames = funcFunction.parameterList.parameterDeclarationList.map { it.identifier.text }
//        return HintInfo.MethodInfo(funcFunctionName, paramNames)
//    }


//    override fun getHintInfo(element: PsiElement): HintInfo? {
//        return HintInfo.MethodInfo("test", listOf("foo", "bar"))
////        val funcFunctionCall = element as? FuncFunctionCall ?: return null
////        println("hint: $funcFunctionCall - ${funcFunctionCall.name}")
////        val funcFunction = funcFunctionCall.functionCallIdentifier.reference ?: return null
////        val resolve = funcFunction.resolve() as? FuncFunction
////        println("resolve: $funcFunction - $resolve")
////        if (resolve == null) return null
////        val funcFunctionName = resolve.name ?: return null
////        val parameters = resolve.parameterList.parameterDeclarationList.map { it.identifier.text }
////        println("fun: $funcFunctionName - $parameters")
////        return HintInfo.MethodInfo(funcFunctionName, parameters)
//    }
}