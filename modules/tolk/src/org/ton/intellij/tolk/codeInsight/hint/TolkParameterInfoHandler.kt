package org.ton.intellij.tolk.codeInsight.hint

import com.intellij.lang.parameterInfo.*
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.util.startOffset
import org.ton.intellij.tolk.psi.*
import org.ton.intellij.tolk.type.TolkTy
import org.ton.intellij.tolk.type.render
import org.ton.intellij.util.parentOfType

class TolkParameterInfoHandler : ParameterInfoHandler<PsiElement, List<String>> {
    override fun findElementForParameterInfo(context: CreateParameterInfoContext): PsiElement? {
        val element = context.file.findElementAt(context.offset) ?: return null
        val typeArgumentList = element.parentOfType<TolkTypeArgumentList>()
        if (typeArgumentList != null) {
            return typeParametersInfo(typeArgumentList, context)
        }

        val argumentList = element.parentOfType<TolkArgumentList>()
        if (argumentList != null) {
            return parametersInfo(argumentList, context)
        }

        return null
    }

    private fun parametersInfo(arguments: TolkArgumentList, context: CreateParameterInfoContext): TolkArgumentList? {
        val callExpression = arguments.parentOfType<TolkCallExpression>() ?: return null

        val parameterInfos: ArrayList<String> = ArrayList()
        iterateOverParameters(callExpression) { parameter, _ ->
            val parameterInfo = buildString {
                if (parameter.isMutable) {
                    append("mutate ")
                }
                append(parameter.name)
                if (parameter is TolkParameter) {
                    append(": ")
                    append((parameter.typeExpression?.type ?: TolkTy.Unknown).render())
                    val parameterDefault = parameter.parameterDefault
                    val defExpr = parameterDefault?.expression?.unwrapParentheses()
                    if (defExpr != null) {
                        append(" = ")
                        when (defExpr) {
                            is TolkStructExpression -> {
                                if (defExpr.structExpressionBody.structExpressionFieldList.isEmpty()) {
                                    append("{}")
                                } else {
                                    append("{...}")
                                }
                            }

                            else -> append(defExpr.text)
                        }
                    }
                }
            }
            parameterInfos.add(parameterInfo)
        }

        context.itemsToShow = arrayOf(parameterInfos)
        return arguments
    }

    private fun typeParametersInfo(typeArguments: TolkTypeArgumentList, context: CreateParameterInfoContext): TolkTypeArgumentList? {
        val typeParameterListOwner = when (val parent = typeArguments.parent) {
            is TolkReferenceExpression     -> parent.reference?.resolve() as? TolkTypeParameterListOwner
            is TolkReferenceTypeExpression -> parent.reference?.resolve() as? TolkTypeParameterListOwner
            is TolkFieldLookup             -> parent.reference?.resolve() as? TolkTypeParameterListOwner
            is TolkMatchPatternReference   -> parent.reference?.resolve() as? TolkTypeParameterListOwner
            else                           -> null
        } ?: return null

        val typeParameters = typeParameterListOwner.typeParameterList?.typeParameterList ?: return null
        val parameterInfos = typeParameters.map { param ->
            buildString {
                append(param.name)
                param.defaultTypeParameter?.typeExpression?.let {
                    append(" = ")
                    append(it.type?.render() ?: it.text)
                }
            }
        }

        context.itemsToShow = arrayOf(parameterInfos)
        return typeArguments
    }

    override fun showParameterInfo(element: PsiElement, context: CreateParameterInfoContext) {
        context.showHint(element, element.textRange.startOffset, this)
    }

    override fun findElementForUpdatingParameterInfo(context: UpdateParameterInfoContext): PsiElement? {
        val element = context.file.findElementAt(context.offset) ?: return null
        return element.parentOfType<TolkArgumentList>() ?: element.parentOfType<TolkTypeArgumentList>()
    }

    override fun updateParameterInfo(parameterOwner: PsiElement, context: UpdateParameterInfoContext) {
        if (context.parameterOwner != parameterOwner) {
            context.removeHint()
            return
        }
        val currentParameterIndex = if (parameterOwner.startOffset == context.offset) {
            -1
        } else {
            ParameterInfoUtils.getCurrentParameterIndex(parameterOwner.node, context.offset, TolkElementTypes.COMMA)
        }
        context.setCurrentParameter(currentParameterIndex)
    }

    override fun updateUI(p: List<String>, context: ParameterInfoUIContext) {
        val range = getArgumentRange(p, context.currentParameterIndex)
        updateUI(presentText(p), range, context)
    }

    private fun getArgumentRange(arguments: List<String>, index: Int): TextRange {
        if (index < 0 || index >= arguments.size) return TextRange.EMPTY_RANGE
        val start = arguments.take(index).sumOf { it.length + 2 }
        val end = start + arguments[index].length
        return TextRange(start, end)
    }

    private fun presentText(params: List<String>): String {
        return if (params.isEmpty()) "<no arguments>" else params.joinToString(", ")
    }

    private fun updateUI(text: String, range: TextRange, context: ParameterInfoUIContext) {
        context.setupUIComponentPresentation(
            text,
            range.startOffset,
            range.endOffset,
            !context.isUIComponentEnabled,
            false,
            false,
            context.defaultParameterColor
        )
    }
}
