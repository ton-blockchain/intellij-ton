package org.ton.intellij.tlb.psi

import org.ton.intellij.tlb.TlbSize

interface TlbSizeOwner : TlbElement {
    val tlbSize: TlbSize? get() = null
}