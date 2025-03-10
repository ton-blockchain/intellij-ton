package org.ton.intellij.tolk.codeInsight.hint

import com.intellij.lang.parameterInfo.*
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import com.intellij.refactoring.suggested.startOffset
import org.ton.intellij.tolk.psi.*
import org.ton.intellij.util.parentOfType

class TolkParameterInfoHandler : ParameterInfoHandler<TolkArgumentList, List<String>> {
    override fun findElementForParameterInfo(context: CreateParameterInfoContext): TolkArgumentList? {
        val argumentList = findArgumentList(context.file, context.offset) ?: return null
        val callExpression = argumentList.parentOfType<TolkCallExpression>() ?: return null
        val callee = callExpression.expression
        val function = when (callee) {
            is TolkReferenceExpression -> {
                callee.reference?.resolve() as? TolkFunction
            }

            is TolkDotExpression -> {
                callee.right?.reference?.resolve() as? TolkFunction
            }

            else -> null
        } ?: return null

        val parameterList = function.parameterList?.parameterList ?: return null
        val parameterIterator = parameterList.iterator()

        val parameterInfos: ArrayList<String> = if (callee is TolkDotExpression && parameterIterator.hasNext()) {
            parameterIterator.next()
            ArrayList(parameterList.size - 1)
        } else {
            ArrayList(parameterList.size)
        }

        while (parameterIterator.hasNext()) {
            val parameter = parameterIterator.next()
            val parameterInfo = buildString {
                append(parameter.name)
                append(": ")
                append(parameter.typeExpression?.text)
            }
            parameterInfos.add(parameterInfo)
        }

        context.itemsToShow = arrayOf(parameterInfos)
        return callExpression.argumentList
    }

    override fun showParameterInfo(
        element: TolkArgumentList,
        context: CreateParameterInfoContext
    ) {
        context.showHint(element, element.textRange.startOffset, this)
    }

    override fun findElementForUpdatingParameterInfo(context: UpdateParameterInfoContext): TolkArgumentList? {
        return findArgumentList(context.file, context.offset)
    }

    override fun updateParameterInfo(
        parameterOwner: TolkArgumentList,
        context: UpdateParameterInfoContext
    ) {
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

    override fun updateUI(
        p: List<String>,
        context: ParameterInfoUIContext
    ) {
        val range = getArgumentRange(p, context.currentParameterIndex)
        updateUI(presentText(p), range, context)
    }

    private fun getArgumentRange(arguments: List<String>, index: Int): TextRange {
        if (index < 0 || index >= arguments.size) return TextRange.EMPTY_RANGE
        val start = arguments.take(index).sumOf { it.length + 2 }
        val end = start + arguments[index].length
        return TextRange(start, end)
    }

    private fun findArgumentList(file: PsiFile, offset: Int): TolkArgumentList? {
        return file.findElementAt(offset)?.parentOfType<TolkArgumentList>()
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
