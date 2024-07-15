package org.ton.intellij.tact.highlighting

import com.intellij.lexer.FlexAdapter
import org.ton.intellij.tact.parser._TactLexer

class TactLexer : FlexAdapter(_TactLexer())
