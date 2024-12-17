package org.ton.intellij.fc2tolk.printing

class FTPrinterBase {
    private val stringBuilder = StringBuilder()
    var currentIndent = 0
    private val indentSymbol = " ".repeat(4)
    var lastSymbolIsLineBreak = false
    private var lastSymbolIsSingleSpace = false

    override fun toString(): String = stringBuilder.toString()

    fun printWithSurroundingSpaces(value: String) {
        print(" ")
        print(value)
        print(" ")
    }

    fun print(value: String) {
        if (value.isEmpty()) return

        if (value == " ") {
            // Don't try to print a single space at the beginning of the line
            if (lastSymbolIsLineBreak) return

            val prevLastSymbolIsSingleSpace = lastSymbolIsSingleSpace
            lastSymbolIsSingleSpace = true

            // Don't try to print multiple single spaces in a row
            if (!prevLastSymbolIsSingleSpace) append(" ")
        } else {
            lastSymbolIsSingleSpace = false
            append(value)

            if (value.length >= 2 && value.last() == ' ' && value[value.lastIndex - 1] != ' ') {
                lastSymbolIsSingleSpace = true
            }
        }
    }

    fun println(lineBreaks: Int = 1) {
        lastSymbolIsSingleSpace = false
        repeat(lineBreaks) { append("\n") }
    }

    inline fun indented(block: () -> Unit) {
        currentIndent++
        block()
        currentIndent--
    }

    inline fun block(body: () -> Unit) {
        par(ParenthesisKind.CURVED) {
            indented(body)
        }
    }

    inline fun par(kind: ParenthesisKind = ParenthesisKind.ROUND, body: () -> Unit) {
        print(kind.open)
        body()
        print(kind.close)
    }

    inline fun <T> renderList(list: Collection<T>, separator: String = ", ", renderElement: (T) -> Unit) =
        renderList(list, { this.print(separator) }, renderElement)

    inline fun <T> renderList(list: Collection<T>, separator: () -> Unit, renderElement: (T) -> Unit) {
        if (list.isEmpty()) return
        var first = true
        for (element in list) {
            if (first) {
                first = false
            } else {
                separator()
            }
            renderElement(element)
        }
    }

    private fun append(text: String) {
        if (lastSymbolIsLineBreak) {
            stringBuilder.append(indentSymbol.repeat(currentIndent))
        }
        stringBuilder.append(text)

        lastSymbolIsLineBreak = stringBuilder.lastOrNull() == '\n'
    }

    enum class ParenthesisKind(val open: String, val close: String) {
        ROUND("(", ")"),
        CURVED("{", "}"),
        ANGLE("<", ">"),
        SQUARE("[", "]")
    }
}