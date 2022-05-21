package org.ton.intellij.func.completion

import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.editor.EditorModificationUtil

fun LookupElementBuilder.insertParenthesis(moveOutside: Boolean, space: Boolean = true): LookupElementBuilder =
    withInsertHandler { ctx, _ ->
        ctx.document.insertString(ctx.selectionEndOffset, (if (space) " " else "") + "()")
        val caretShift = if (moveOutside) {
            if (space) 3 else 2
        } else {
            if (space) 2 else 1
        }
        EditorModificationUtil.moveCaretRelatively(ctx.editor, caretShift)
    }

fun LookupElementBuilder.insertBraces(): LookupElementBuilder =
    withInsertHandler { ctx, _ ->
        ctx.document.insertString(ctx.selectionEndOffset, " {\n}")
        EditorModificationUtil.moveCaretRelatively(ctx.editor, 2)
    }

fun LookupElementBuilder.insertSpace(): LookupElementBuilder =
    withInsertHandler { ctx, _ ->
        ctx.document.insertString(ctx.selectionEndOffset, " ")
        EditorModificationUtil.moveCaretRelatively(ctx.editor, 1)
    }