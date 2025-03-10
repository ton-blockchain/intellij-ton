package org.ton.intellij.tolk.parser

import com.intellij.lang.PsiBuilder
import com.intellij.lang.WhitespacesAndCommentsBinder
import com.intellij.lang.parser.GeneratedParserUtilBase
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import org.ton.intellij.tolk.parser.TolkParserDefinition.Companion.DOC_COMMENT
import org.ton.intellij.tolk.parser.TolkParserDefinition.Companion.EOL_COMMENT
import org.ton.intellij.tolk.psi.TolkElementTypes

object TolkParserUtil : GeneratedParserUtilBase() {
    @JvmField
    val ADJACENT_LINE_COMMENTS = WhitespacesAndCommentsBinder { tokens, _, getter ->
        var candidate = tokens.size
        for (i in 0 until tokens.size) {
            val token = tokens[i]
            if (DOC_COMMENT == token) {
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
    fun getKeyword(b: PsiBuilder, level: Int): Boolean {
        return softKeyword(b, "get", TolkElementTypes.GET_KEYWORD)
    }

    private fun softKeyword(b: PsiBuilder, keyword: String, elementType: IElementType): Boolean {
        val tokenType = b.tokenType
        if (tokenType == elementType) {
            b.advanceLexer()
            return true
        }
        if (tokenType == TolkElementTypes.IDENTIFIER && b.tokenText == keyword) {
            b.remapCurrentToken(elementType)
            b.advanceLexer()
            return true
        }
        return false
    }
}
