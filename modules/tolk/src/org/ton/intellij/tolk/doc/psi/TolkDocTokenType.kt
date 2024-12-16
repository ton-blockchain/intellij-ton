package org.ton.intellij.tolk.doc.psi

import com.intellij.psi.impl.source.tree.CompositeElement
import com.intellij.psi.tree.ICompositeElementType
import com.intellij.psi.tree.IElementType
import org.ton.intellij.tolk.psi.TolkTokenType

class TolkDocTokenType(debugName: String) : TolkTokenType(debugName)

class TolkDocCompositeTokenType(
    debugName: String,
    private val astFactory: (IElementType) -> CompositeElement,
) : TolkTokenType(debugName), ICompositeElementType {
    override fun createCompositeNode(): CompositeElement = astFactory(this)
}
