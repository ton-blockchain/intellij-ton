package org.ton.intellij.tlb.psi.impl

import com.intellij.lang.ASTNode
import org.ton.intellij.tlb.psi.TlbNamedElementImpl
import org.ton.intellij.tlb.psi.TlbResultType

abstract class TlbResultTypeMixin(node: ASTNode) : TlbNamedElementImpl(node), TlbResultType