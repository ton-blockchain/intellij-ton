package org.ton.intellij.asm

import com.intellij.lexer.FlexAdapter
import org.ton.intellij.asm.parser._AsmLexer

class AsmLexer : FlexAdapter(_AsmLexer())
