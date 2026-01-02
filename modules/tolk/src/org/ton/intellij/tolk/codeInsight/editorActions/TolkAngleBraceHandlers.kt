/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 *
 * Code from: https://github.com/intellij-rust/intellij-rust
 */

package org.ton.intellij.tolk.codeInsight.editorActions

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.EditorEx
import org.ton.intellij.tolk.psi.TolkElementTypes.*
import org.ton.intellij.util.tokenSetOf

private val GENERIC_NAMED_ENTITY_KEYWORDS = tokenSetOf(FUN_KEYWORD, STRUCT_KEYWORD, TYPE_KEYWORD)

private val INVALID_INSIDE_TOKENS = tokenSetOf(LBRACE, RBRACE, SEMICOLON)

class TolkAngleBraceTypedHandler : TolkBraceTypedHandler(AngleBraceHandler)

class TolkAngleBraceBackspaceHandler : TolkBraceBackspaceHandler(AngleBraceHandler)

object AngleBraceHandler : BraceHandler {

    override val opening: BraceKind = BraceKind('<', LT)
    override val closing: BraceKind = BraceKind('>', GT)

    override fun shouldComplete(editor: Editor): Boolean {
        val offset = editor.caretModel.offset
        val lexer = editor.createLexer(offset - 1) ?: return false

        return when (lexer.tokenType) {
            IDENTIFIER -> {
                // don't complete angle braces inside identifier
                if (lexer.end != offset) return false
                // it considers that a typical case is only one whitespace character
                // between keyword (fn, enum, etc.) and identifier
                if (lexer.start > 1) {
                    lexer.retreat()
                    lexer.retreat()
                    if (lexer.tokenType in GENERIC_NAMED_ENTITY_KEYWORDS) return true
                    lexer.advance()
                    lexer.advance()
                }

                // assume `foo<` case
                true
            }

            else       -> false
        }
    }

    override fun calculateBalance(editor: Editor): Int {
        val offset = editor.caretModel.offset - 1
        val iterator = (editor as EditorEx).highlighter.createIterator(offset)
        while (iterator.start > 0 && iterator.tokenType !in INVALID_INSIDE_TOKENS) {
            iterator.retreat()
        }

        if (iterator.tokenType in INVALID_INSIDE_TOKENS) {
            iterator.advance()
        }

        var balance = 0
        while (!iterator.atEnd() && balance >= 0 && iterator.tokenType !in INVALID_INSIDE_TOKENS) {
            when (iterator.tokenType) {
                LT -> balance++
                GT -> balance--
            }
            iterator.advance()
        }
        return balance
    }
}
