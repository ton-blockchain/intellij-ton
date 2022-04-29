package org.ton.intellij.func.completion

import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.editor.EditorModificationUtil

fun LookupElementBuilder.insertParenthesis(moveOutside: Boolean): LookupElementBuilder =
    withInsertHandler { ctx, _ ->
        ctx.document.insertString(ctx.selectionEndOffset, "()")
        val caretShift = if (moveOutside) 2 else 1
        EditorModificationUtil.moveCaretRelatively(ctx.editor, caretShift)
    }