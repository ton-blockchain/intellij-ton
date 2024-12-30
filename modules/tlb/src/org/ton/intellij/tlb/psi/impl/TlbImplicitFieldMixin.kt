package org.ton.intellij.tlb.psi.impl

import com.intellij.lang.ASTNode
import org.ton.intellij.tlb.psi.TlbImplicitField
import org.ton.intellij.tlb.psi.TlbNamedElementImpl

abstract class TlbImplicitFieldMixin(node: ASTNode) : TlbNamedElementImpl(node), TlbImplicitField {

}