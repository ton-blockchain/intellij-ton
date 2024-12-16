package org.ton.intellij.fc2tolk.printing

import org.ton.intellij.fc2tolk.ConverterContext
import org.ton.intellij.fc2tolk.tree.*
import org.ton.intellij.fc2tolk.tree.visitors.FTVisitor

class FTCodeBuilder(
    private val context: ConverterContext
) {
    private val printer = FTPrinterBase()

    fun printCode(root: FTTreeElement): String {
        Visitor().also { root.accept(it) }
        return printer.toString().replace("\r\n", "\n")
    }

    private inner class Visitor : FTVisitor() {
        override fun visitTreeElement(element: FTElement) {
            printer.print("/* !!! Hit visitElement for element type: ${element::class} !!! */")
        }

        override fun visitTreeRoot(treeRoot: FTTreeRoot) {
            treeRoot.element.accept(this)
        }

        override fun visitFile(file: FTFile) {
            file.declarationList.forEach { it.accept(this) }
        }

        override fun visitNameIdentifier(nameIdentifier: FTNameIdentifier) {
            printer.print(nameIdentifier.value.escapedTolkId())
        }

        override fun visitMethodId(methodId: FTMethodId) {
            val value = methodId.value
            if (value != null) {
                printer.print("@methodId(")
                printer.print(value)
                printer.print(")")
            }
        }

        override fun visitFunction(function: FTFunction) {
            var isGetFun = false
            function.functionModifiers.forEach {
                it.accept(this)
                if (it is FTMethodId && it.value == null) {
                    isGetFun = true
                }
            }

            if (isGetFun) {
                printer.printWithSurroundingSpaces("get")
            }
            printer.printWithSurroundingSpaces("fun")

            function.name.accept(this)

            if (function.typeParameterList.typeParameters.isNotEmpty()) {
                function.typeParameterList.accept(this)
            }




        }

    }
}