package org.ton.intellij.tlb.psi



fun TlbFieldList.print(
    appendable: Appendable,
    showBraces: Boolean = true,
) {
    fieldList.forEachIndexed { index, field ->
        if (index > 0) {
            appendable.append(" ")
        }
        field.print(appendable, showBraces)
    }
}

fun TlbField.print(
    appendable: Appendable,
    showBraces: Boolean = true,
) {
    when (this) {
        is TlbImplicitField -> {
            if (showBraces) {
                appendable.append("{")
            }
            appendable.append(identifier.text)
            appendable.append(":")
            when {
                typeKeyword != null -> appendable.append("Type")
                tag != null -> appendable.append("#")
            }
            if (showBraces) {
                appendable.append("}")
            }
        }

        is TlbConstraintField -> {
            if (showBraces) {
                appendable.append("{")
            }
            typeExpression.print(appendable)
            if (showBraces) {
                appendable.append("}")
            }
        }

        is TlbCommonField -> {
            val name = identifier?.text
            if (name != null) {
                appendable.append(name)
                appendable.append(":")
            }
            typeExpression.print(appendable, skipParens = !showBraces)
        }
    }
}

fun TlbConstructor.printToString(
    skipTag: Boolean = false,
    showBraces: Boolean = true,
) = buildString { print(this, skipTag, showBraces) }
fun TlbConstructor.print(
    appendable: Appendable,
    skipTag: Boolean = false,
    implicitBraces: Boolean = true,
) {
    appendable.append(name)
    if (!skipTag) {
        val constructorTag = constructorTag?.text
        if (constructorTag == null) {
            appendable.append("(invalid-constructor-tag)")
        }
        appendable.append(constructorTag)
    }
    val fieldList = fieldList
    if (fieldList == null) {
        appendable.append("(invalid-constructor-field-list)")
        return
    }
    if (fieldList.fieldList.isNotEmpty()) {
        appendable.append(" ")
    }
    fieldList.print(appendable, implicitBraces)
    appendable.append(" = ")

    val resultType = resultType
    if (resultType == null) {
        appendable.append("(invalid-constructor-result-type)")
        return
    }
    appendable.append(resultType.name)
    val params = resultType.paramList.typeExpressionList
    params.forEach {
        appendable.append(" ")
//        if (it is TlbNegatedTypeExpression && it.typeExpression !is TlbIntTypeExpression) {
//            appendable.append("~") // TL-B bug?
//        }
        it.print(appendable, 100, !implicitBraces)
    }
}

fun TlbTypeExpression.print(
    appendable: Appendable,
    priority: Int = 0,
    skipParens: Boolean = false
) {
    if (priority > 0 && skipParens) {
        return print(appendable, 0, true)
    }
    when (this) {
        is TlbNegatedTypeExpression -> {
            appendable.append("~")
            this.typeExpression.print(appendable, priority, skipParens)
        }

        is TlbParamTypeExpression -> {
            appendable.append(text)
        }

        is TlbConstructorTypeExpression -> {
            val fieldList = fieldList
            if (fieldList == null) {
                appendable.append("(invalid-constructor-type-expression)")
                return
            }
            appendable.append("[")
            fieldList.print(appendable)
            appendable.append("]")
        }

        is TlbApplyTypeExpression -> {
            val arguments = argumentList.typeExpressionList
            val showParens = priority > 90 && arguments.isNotEmpty()
            if (showParens) {
                appendable.append("(")
            }
            typeExpression.print(appendable, 91)
            arguments.forEachIndexed { index, argument ->
                appendable.append(" ")
                argument.print(appendable, 91, skipParens)
            }
            if (showParens) {
                appendable.append(")")
            }
        }

        is TlbAddTypeExpression -> {
            val args = typeExpressionList
            if (args.size != 2) {
                appendable.append("(invalid-add-type-expression)")
                return
            }
            if (priority > 20) {
                appendable.append("(")
            }
            args[0].print(appendable, 20, skipParens)
            appendable.append(" + ")
            args[1].print(appendable, 21, skipParens)
            if (priority > 20) {
                appendable.append(")")
            }
        }

        is TlbGetBitTypeExpression -> {
            val args = typeExpressionList
            if (args.size != 2) {
                appendable.append("(invalid-get-bit-type-expression)")
                return
            }
            if (priority > 97) {
                appendable.append("(")
            }
            args[0].print(appendable, 98, skipParens)
            appendable.append(".")
            args[1].print(appendable, 98, skipParens)
            if (priority > 97) {
                appendable.append(")")
            }
        }

        is TlbConstraintTypeExpression -> {
            val operator = children.getOrNull(1)
            if (operator == null) {
                appendable.append("(invalid-constraint-type-expression)")
                return
            }
            var arguments = typeExpressionList
            val showParens = priority > 90 && arguments.isNotEmpty()
            if (showParens) {
                appendable.append("(")
            }
            when {
                equals != null -> {
                    appendable.append("=")
                }
                geq != null -> {
                    appendable.append("<=")
                    arguments = arguments.reversed()
                }
                greater != null -> {
                    appendable.append("<")
                    arguments = arguments.reversed()
                }
                leq != null -> {
                    appendable.append("<=")
                }
                less != null -> {
                    appendable.append("<")
                }
                else -> "??"
            }
            arguments.forEachIndexed { index, argument ->
                appendable.append(" ")
                argument.print(appendable, 0, skipParens)
            }
            if (showParens) {
                appendable.append(")")
            }
        }

        is TlbIntTypeExpression -> {
            appendable.append(text)
        }

        is TlbMulTypeExpression -> {
            var args = typeExpressionList
            if (args.size != 2) {
                appendable.append("(invalid-mul-type-expression)")
                return
            }
            if (args[1].unwrap() is TlbIntTypeExpression) {
                args = args.reversed()
            }
            if (priority > 30) {
                appendable.append("(")
            }
            args[0].print(appendable, 30, skipParens)
            appendable.append(" * ")
            args[1].print(appendable, 31, skipParens)
            if (priority > 30) {
                appendable.append(")")
            }
        }

        is TlbCondTypeExpression -> {
            val args = typeExpressionList
            if (args.size != 2) {
                appendable.append("(invalid-cond-type-expression)")
                return
            }
            if (priority > 95) {
                appendable.append("(")
            }
            args[0].print(appendable, 96, skipParens)
            appendable.append(" ? ")
            args[1].print(appendable, 96, skipParens)
            if (priority > 95) {
                appendable.append(")")
            }
        }

        is TlbReferenceTypeExpression -> {
            val arg = typeExpression
            if (arg == null) {
                appendable.append("(invalid-reference-type-expression)")
                return
            }
            appendable.append("^")
            arg.print(appendable, 100, skipParens)
        }

        is TlbParenTypeExpression -> {
            unwrap()?.print(appendable, priority)
        }

        else -> {
            appendable.append("(unknown-type)")
        }
    }
}