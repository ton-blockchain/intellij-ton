package com.github.andreypfau.intellijton.func.parser

import com.intellij.lexer.FlexAdapter

class FuncLexerAdapter : FlexAdapter(_FuncLexer(null))