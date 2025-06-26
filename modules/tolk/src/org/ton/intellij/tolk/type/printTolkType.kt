package org.ton.intellij.tolk.type

import com.intellij.codeInsight.hints.declarative.PresentationTreeBuilder
import org.ton.intellij.tolk.type.TolkTy.Companion.Null
import org.ton.intellij.util.printPsi

fun PresentationTreeBuilder.printTolkType(type: TolkTy) {
    when (type) {
        is TolkTyParam -> {
            printPsi(type.parameter.psi, type.name ?: "<unknown>")
        }
        is TolkTyAlias -> {
            printPsi(type.psi, type.psi.name ?: type.render())
        }
        is TolkStructTy -> {
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
        is TolkTyFunction -> {
            val inputType = type.parametersType
            text("(")
            var separator = ""
            inputType.forEach {
                if (separator.isNotEmpty()) {
                    text(separator)
                }
                separator = ", "
                printTolkType(it)
            }
            text(")")
            text(" -> ")
            val returnType = type.returnType
            printTolkType(returnType)
        }

        is TolkTyTensor -> {
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

        is TolkTyTypedTuple -> {
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

        is TolkTyUnion -> {
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

        else -> text(type.render())
    }
}
