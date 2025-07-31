package org.ton.intellij.tlb.psi.impl

import com.intellij.lang.ASTNode
import org.ton.intellij.tlb.psi.TlbCommonField
import org.ton.intellij.tlb.psi.TlbConstructor
import org.ton.intellij.tlb.psi.TlbNamedElementImpl
import org.ton.intellij.tlb.psi.TlbTypeExpression

abstract class TlbConstructorMixin(node: ASTNode) : TlbNamedElementImpl(node), TlbConstructor
