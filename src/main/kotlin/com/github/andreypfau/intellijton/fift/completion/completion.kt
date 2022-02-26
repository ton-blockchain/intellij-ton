package com.github.andreypfau.intellijton.fift.completion

import com.github.andreypfau.intellijton.fift.psi.FiftFile
import com.github.andreypfau.intellijton.fift.psi.FiftTypes.*
import com.github.andreypfau.intellijton.fift.resolve.resolveAllWordDefStatements
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.editor.EditorModificationUtil
import com.intellij.patterns.PlatformPatterns
import com.intellij.util.ProcessingContext

class FiftWordCompletionContributor : CompletionContributor() {
    init {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(IDENTIFIER), FuncBasicWordCompletionProvider())
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(IDENTIFIER), FuncWordCompletionProvider())
    }
}

class FuncBasicWordCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        if (parameters.position.text == CompletionInitializationContext.DUMMY_IDENTIFIER_TRIMMED) return
        sequenceOf(
            INCLUDE,
            IF, IFNOT, COND,
            TRUE, FALSE,
            DUP, DROP, SWAP, ROT, REV_ROT, OVER, TUCK, NIP, DUP_DUP,
            DROP_DROP, SWAP_SWAP, PICK, ROLL, REV_ROLL, EXCH, EXCH2, COND_DUP
        ).map {
            LookupElementBuilder.create(it).insertSpace()
        }.forEach {
            result.addElement(it)
        }
    }
}

class FuncWordCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        if (parameters.position.text == CompletionInitializationContext.DUMMY_IDENTIFIER_TRIMMED) return
        val file = parameters.originalFile as? FiftFile ?: return
        val textOffset = parameters.position.textOffset
        file.resolveAllWordDefStatements().filter {
            it.textOffset < textOffset
        }.map { wordDef ->
            LookupElementBuilder.createWithIcon(wordDef).insertSpace()
        }.forEach {
            result.addElement(it)
        }
    }
}

fun LookupElementBuilder.insertSpace() = withInsertHandler { ctx, _ ->
    ctx.document.insertString(ctx.selectionEndOffset, " ")
    EditorModificationUtil.moveCaretRelatively(ctx.editor, 1)
}