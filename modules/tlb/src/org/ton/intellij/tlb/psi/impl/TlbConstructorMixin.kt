package org.ton.intellij.tlb.psi.impl

import com.intellij.lang.ASTNode
import org.ton.intellij.tlb.psi.TlbConstructor
import org.ton.intellij.tlb.psi.TlbNamedElementImpl

abstract class TlbConstructorMixin(node: ASTNode) : TlbNamedElementImpl(node), TlbConstructor
