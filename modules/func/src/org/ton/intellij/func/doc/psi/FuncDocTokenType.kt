package org.ton.intellij.func.doc.psi

import com.intellij.psi.impl.source.tree.CompositeElement
import com.intellij.psi.tree.ICompositeElementType
import com.intellij.psi.tree.IElementType
import org.ton.intellij.func.psi.FuncTokenType

class FuncDocTokenType(debugName: String) : FuncTokenType(debugName)

class FuncDocCompositeTokenType(
    debugName: String,
    private val astFactory: (IElementType) -> CompositeElement,
) : FuncTokenType(debugName), ICompositeElementType {
    override fun createCompositeNode(): CompositeElement = astFactory(this)
}
