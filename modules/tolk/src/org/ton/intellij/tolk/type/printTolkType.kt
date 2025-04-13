package org.ton.intellij.tolk.type

import com.intellij.codeInsight.hints.declarative.PresentationTreeBuilder
import org.ton.intellij.tolk.type.TolkType.Companion.Null
import org.ton.intellij.util.printPsi

fun PresentationTreeBuilder.printTolkType(type: TolkType) {
    when (type) {
        is TolkType.ParameterType -> {
            printPsi(type.psiElement, type.name)
        }
        is TolkAliasType -> {
            printPsi(type.psi, type.psi.name ?: buildString {
                type.printDisplayName(this)
            })
        }
        is TolkStructType -> {
            printPsi(type.psi, type.psi.name ?: buildString {
                type.printDisplayName(this)
            })
        }

        is TolkFunctionType -> {
            val inputType = type.inputType
            if (inputType is TolkTensorType || inputType is TolkUnitType) {
                printTolkType(inputType)
            } else {
                text("(")
                printTolkType(inputType)
                text(")")
            }
            text(" -> ")
            val returnType = type.returnType
            if (returnType is TolkUnitType) {
                text("void")
            } else {
                printTolkType(returnType)
            }
        }

        is TolkTensorType -> {
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

        is TolkTypedTupleType -> {
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

        is TolkUnionType -> {
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
            type.printDisplayName(this)
        })
    }
}
