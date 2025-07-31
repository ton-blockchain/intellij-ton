package org.ton.intellij.tlb.psi.impl

import com.intellij.lang.ASTNode
import org.ton.intellij.tlb.psi.TlbCommonField
import org.ton.intellij.tlb.psi.TlbNamedElementImpl

abstract class TlbCommonFieldMixin(node: ASTNode) : TlbNamedElementImpl(node), TlbCommonField