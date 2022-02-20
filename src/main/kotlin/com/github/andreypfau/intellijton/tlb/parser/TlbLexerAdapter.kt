package com.github.andreypfau.intellijton.tlb.parser

import com.intellij.lexer.FlexAdapter

class TlbLexerAdapter : FlexAdapter(_TlbLexer(null))