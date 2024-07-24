package org.ton.intellij.tlb.psi

import com.intellij.psi.tree.IElementType
import org.ton.intellij.tlb.TlbLanguage

open class TlbTokenType(val name: String) : IElementType(name, TlbLanguage)
