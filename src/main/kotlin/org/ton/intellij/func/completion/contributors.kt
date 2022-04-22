package org.ton.intellij.func.completion

import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.editor.EditorModificationUtil

fun LookupElementBuilder.insertParenthesis(insertSemicolon: Boolean, moveOutside: Boolean) =
    withInsertHandler { ctx, _ ->
        ctx.document.insertString(ctx.selectionEndOffset, if (insertSemicolon) "();" else "()")
        val caretShift = if (moveOutside) {
            if (insertSemicolon) 3 else 2
        } else 1
        EditorModificationUtil.moveCaretRelatively(ctx.editor, caretShift)
    }
