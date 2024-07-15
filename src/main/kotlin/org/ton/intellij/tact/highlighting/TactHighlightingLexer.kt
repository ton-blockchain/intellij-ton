package org.ton.intellij.tact.highlighting

import com.intellij.lexer.LayeredLexer
import com.intellij.lexer.StringLiteralLexer
import org.ton.intellij.tact.psi.TactElementTypes

class TactHighlightingLexer : LayeredLexer(TactLexer()) {
    init {
        registerLayer(
            StringLiteralLexer(
                '"',
                TactElementTypes.STRING_LITERAL
            )
        )

    }
}
