package org.ton.intellij.tolk.parser

import com.intellij.lang.PsiBuilder
import com.intellij.lang.WhitespacesAndCommentsBinder
import com.intellij.lang.parser.GeneratedParserUtilBase
import com.intellij.psi.TokenType
import org.ton.intellij.tolk.parser.TolkParserDefinition.Companion.BLOCK_DOC_COMMENT
import org.ton.intellij.tolk.parser.TolkParserDefinition.Companion.EOL_COMMENT
import org.ton.intellij.tolk.parser.TolkParserDefinition.Companion.EOL_DOC_COMMENT
import org.ton.intellij.tolk.psi.TolkElementTypes

object TolkParserUtil : GeneratedParserUtilBase() {
    @JvmField
    val ADJACENT_LINE_COMMENTS = WhitespacesAndCommentsBinder { tokens, _, getter ->
        var candidate = tokens.size
        for (i in 0 until tokens.size) {
            val token = tokens[i]
            if (BLOCK_DOC_COMMENT == token || EOL_DOC_COMMENT == token) {
                candidate = minOf(candidate, i)
                break
            }
            if (EOL_COMMENT == token) {
                candidate = minOf(candidate, i)
            }
            if (TokenType.WHITE_SPACE == token && "\n\n" in getter[i]) {
                candidate = tokens.size
            }
        }
        candidate
    }

    @JvmStatic
    fun isSpecialIdentifier(b: PsiBuilder, level: Int): Boolean {
        return b.tokenType == TolkElementTypes.IDENTIFIER && when (b.tokenText?.firstOrNull()) {
            '~', '.' -> true
            else -> false
        }
    }

    @JvmStatic
    fun isRegularIdentifier(b: PsiBuilder, level: Int): Boolean {
        return b.tokenType == TolkElementTypes.IDENTIFIER && when (b.tokenText?.firstOrNull()) {
            '~', '.' -> false
            else -> true
        }
    }
}
