package org.ton.intellij.fc2tolk

import org.ton.intellij.fc2tolk.tree.FTTreeElement
import org.ton.intellij.fc2tolk.tree.applyRecursive

interface ConverterContext

abstract class Conversion {
    abstract fun run(treeRoot: FTTreeElement, context: ConverterContext)
}

abstract class RecursiveConversion(context: ConverterContext) : Conversion() {
    override fun run(treeRoot: FTTreeElement, context: ConverterContext) {
        val root = applyToElement(treeRoot)
        assert(root === treeRoot)
    }

    abstract fun applyToElement(element: FTTreeElement): FTTreeElement

    protected fun <E : FTTreeElement> recurse(element: E): E =
        applyRecursive(element, data = null) { it, _ -> applyToElement(it) }
}

