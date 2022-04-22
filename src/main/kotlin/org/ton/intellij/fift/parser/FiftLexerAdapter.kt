package org.ton.intellij.fift.parser

import com.intellij.lexer.FlexAdapter

class FiftLexerAdapter : FlexAdapter(_FiftLexer(null))