package org.ton.intellij.tlb.parser

import com.intellij.lexer.FlexAdapter

class TlbLexerAdapter : FlexAdapter(_TlbLexer(null))