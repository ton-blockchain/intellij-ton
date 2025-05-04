package org.ton.intellij.tolk.type

import com.intellij.codeInsight.hints.declarative.PresentationTreeBuilder
import org.ton.intellij.tolk.type.TolkTy.Companion.Null
import org.ton.intellij.util.printPsi

fun PresentationTreeBuilder.printTolkType(type: TolkTy) {
    when (type) {
        is TyTypeParameter -> {
            printPsi(type.parameter.psi, type.name ?: "<unknown>")
        }
        is TolkAliasTy -> {
            printPsi(type.psi, type.psi.name ?: buildString {
                type.renderAppendable(this)
            })
        }
        is TyStruct -> {
            val psi = type.psi
            printPsi(psi, psi.name ?: "<unknown>")
            if (type.typeArguments.isNotEmpty()) {
                text("<")
                val iterator = type.typeArguments.iterator()
                while (iterator.hasNext()) {
                    val argument = iterator.next()
                    printTolkType(argument)
                    if (iterator.hasNext()) {
                        text(", ")
                    }
                }
                text(">")
            }
        }
        is TolkFunctionTy -> {
            val inputType = type.inputType
            if (inputType is TolkTensorTy || inputType is TolkUnitTy) {
                printTolkType(inputType)
            } else {
                text("(")
                printTolkType(inputType)
                text(")")
            }
            text(" -> ")
            val returnType = type.returnType
            if (returnType is TolkUnitTy) {
                text("void")
            } else {
                printTolkType(returnType)
            }
        }

        is TolkTensorTy -> {
            text("(")
            val iterator = type.elements.iterator()
            while (iterator.hasNext()) {
                val element = iterator.next()
                printTolkType(element)
                if (iterator.hasNext()) {
                    text(", ")
                }
            }
            text(")")
        }

        is TolkTypedTupleTy -> {
            text("[")
            val iterator = type.elements.iterator()
            while (iterator.hasNext()) {
                val element = iterator.next()
                printTolkType(element)
                if (iterator.hasNext()) {
                    text(", ")
                }
            }
            text("]")
        }

        is TyUnion -> {
            val elements = type.variants
            if (elements.size == 2) {
                val first = elements.first()
                val second = elements.last()
                if (first == Null) {
                    printTolkType(second)
                    text("?")
                    return
                }
                if (second == Null) {
                    printTolkType(first)
                    text("?")
                    return
                }
            }
            val iterator = elements.iterator()
            while (iterator.hasNext()) {
                val element = iterator.next()
                printTolkType(element)
                if (iterator.hasNext()) {
                    text(" | ")
                }
            }
        }

        else -> text(buildString {
            type.renderAppendable(this)
        })
    }
}
