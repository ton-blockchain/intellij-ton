package com.github.andreypfau.intellijton.func.completion

import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.editor.EditorModificationUtil

fun LookupElementBuilder.insertParenthesis(insertSemicolon: Boolean) =
    withInsertHandler { ctx, _ ->
        ctx.document.insertString(ctx.selectionEndOffset, if (insertSemicolon) "();" else "()")
        EditorModificationUtil.moveCaretRelatively(ctx.editor, 1)
    }
