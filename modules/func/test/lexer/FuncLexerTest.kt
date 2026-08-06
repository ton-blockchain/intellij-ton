package org.ton.intellij.func.lexer

import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import org.ton.intellij.func.FuncTestBase
import org.ton.intellij.func.psi.FuncElementTypes

class FuncLexerTest : FuncTestBase() {
    fun `test underscore discard before assignment is not lexed as escaped identifier`() {
        val tokens = lex("var (v110, _) = __dict_method_100(uint48, v42, uint48 / 1000, now());")

        assertFalse(tokens.any { it == Token(FuncElementTypes.IDENTIFIER, "_) = _") })
        assertEquals(
            listOf(
                Token(FuncElementTypes.VAR_KEYWORD, "var"),
                Token(FuncElementTypes.LPAREN, "("),
                Token(FuncElementTypes.IDENTIFIER, "v110"),
                Token(FuncElementTypes.COMMA, ","),
                Token(FuncElementTypes.UNDERSCORE, "_"),
                Token(FuncElementTypes.RPAREN, ")"),
                Token(FuncElementTypes.EQ, "="),
                Token(FuncElementTypes.IDENTIFIER, "__dict_method_100"),
                Token(FuncElementTypes.LPAREN, "("),
                Token(FuncElementTypes.IDENTIFIER, "uint48"),
                Token(FuncElementTypes.COMMA, ","),
                Token(FuncElementTypes.IDENTIFIER, "v42"),
                Token(FuncElementTypes.COMMA, ","),
                Token(FuncElementTypes.IDENTIFIER, "uint48"),
                Token(FuncElementTypes.DIV, "/"),
                Token(FuncElementTypes.INTEGER_LITERAL, "1000"),
                Token(FuncElementTypes.COMMA, ","),
                Token(FuncElementTypes.IDENTIFIER, "now"),
                Token(FuncElementTypes.LPAREN, "("),
                Token(FuncElementTypes.RPAREN, ")"),
                Token(FuncElementTypes.RPAREN, ")"),
                Token(FuncElementTypes.SEMICOLON, ";"),
            ),
            tokens,
        )
    }

    fun `test underscore escaped operator identifiers still lex as identifiers`() {
        assertEquals(
            listOf(
                Token(FuncElementTypes.IDENTIFIER, "_+_"),
                Token(FuncElementTypes.IDENTIFIER, "_==_"),
                Token(FuncElementTypes.IDENTIFIER, "_<=>_"),
                Token(FuncElementTypes.IDENTIFIER, "_~%_"),
            ),
            lex("_+_ _==_ _<=>_ _~%_"),
        )
    }

    private fun lex(text: String): List<Token> {
        val lexer = FuncLexer()
        lexer.start(text)

        val tokens = mutableListOf<Token>()
        while (lexer.tokenType != null) {
            if (lexer.tokenType != TokenType.WHITE_SPACE) {
                tokens += Token(lexer.tokenType, lexer.tokenText)
            }
            lexer.advance()
        }
        return tokens
    }

    private data class Token(val type: IElementType?, val text: String)
}
