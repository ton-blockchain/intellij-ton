package org.ton.intellij.tolk.presentation

import org.ton.intellij.tolk.psi.*

fun TolkPsiRenderer.renderParameterList(element: TolkParameterList) = buildString {
    appendParameterList(this, element)
}

fun TolkPsiRenderer.renderTypeExpression(element: TolkTypeExpression) = buildString {
    appendTypeExpression(this, element)
}

open class TolkPsiRenderer {
    fun appendParameterList(
        appendable: Appendable,
        element: TolkParameterList
    ) {
        appendable.append("(")
        val selfParameter = element.selfParameter
        val parameterList = element.parameterList
        if (selfParameter != null) {
            appendSelfParameter(appendable, selfParameter)
            if (parameterList.isNotEmpty()) {
                appendable.append(", ")
            }
        }
        var separator = ""
        for (parameter in parameterList) {
            appendable.append(separator)
            appendParameter(appendable, parameter)
            separator = ", "
        }
        appendable.append(")")
    }

    fun appendSelfParameter(
        appendable: Appendable,
        element: TolkSelfParameter
    ) {
        if (element.isMutable) {
            appendable.append("mutable ")
        }
        appendable.append("self")
    }

    fun appendParameter(
        appendable: Appendable,
        element: TolkParameter
    ) {
        if (element.isMutable) {
            appendable.append("mutable ")
        }
        appendable.append(element.name)
        element.typeExpression?.let {
            appendable.append(": ")
            appendTypeExpression(appendable, it)
        }
    }

    fun appendTypeExpression(
        appendable: Appendable,
        element: TolkTypeExpression
    ) {
        when(element) {
            is TolkFunTypeExpression -> {
                val types = element.typeExpressionList
                types.getOrNull(0)?.let {
                    appendTypeExpression(appendable, it)
                }
                appendable.append(" -> ")
                types.getOrNull(1)?.let {
                    appendTypeExpression(appendable, it)
                }
            }
            is TolkUnionTypeExpression -> {
                var separator = ""
                for (type in element.typeExpressionList) {
                    appendable.append(separator)
                    appendTypeExpression(appendable, type)
                    separator = " | "
                }
            }
            is TolkNullableTypeExpression -> {
                appendTypeExpression(appendable, element.typeExpression)
                appendable.append("?")
            }
            is TolkAutoTypeExpression -> appendable.append("auto")
            is TolkNullTypeExpression -> appendable.append("null")
            is TolkParenTypeExpression -> {
                appendable.append("(")
                element.typeExpression?.let {
                    appendTypeExpression(appendable, it)
                }
                appendable.append(")")
            }
            is TolkTupleTypeExpression -> {
                var separator = ""
                for (type in element.typeExpressionList) {
                    appendable.append(separator)
                    appendTypeExpression(appendable, type)
                    separator = ", "
                }
            }
            is TolkTensorTypeExpression -> {
                var separator = ""
                appendable.append("(")
                for (type in element.typeExpressionList) {
                    appendable.append(separator)
                    appendTypeExpression(appendable, type)
                    separator = ", "
                }
                appendable.append(")")
            }
            is TolkReferenceTypeExpression -> {
                appendable.append(element.referenceName?.removeSurrounding("`"))
                element.typeArgumentList?.typeExpressionList?.let {
                    appendable.append("<")
                    var separator = ""
                    for (type in it) {
                        appendable.append(separator)
                        appendTypeExpression(appendable, type)
                        separator = ", "
                    }
                    appendable.append(">")
                }
            }
        }
    }
}
