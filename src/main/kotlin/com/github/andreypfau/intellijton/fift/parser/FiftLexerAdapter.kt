package com.github.andreypfau.intellijton.fift.parser

import com.intellij.lexer.FlexAdapter

class FiftLexerAdapter : FlexAdapter(_FiftLexer(null))