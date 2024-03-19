package org.ton.intellij.func.psi

import com.intellij.patterns.PatternCondition
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.PsiElementPattern
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.util.ProcessingContext
import org.ton.intellij.util.prevVisibleOrNewLine
import org.ton.intellij.util.psiElement

object FuncPsiPattern {
    fun baseDeclarationPattern(): PsiElementPattern.Capture<PsiElement> =
        psiElement<PsiElement>()
            .withParent(psiElement<FuncFile>())

    fun onStatementBeginning(vararg startWords: String): PsiElementPattern.Capture<PsiElement> =
        PlatformPatterns.psiElement().with(OnStatementBeginning(*startWords))

    private fun identifierStatementBeginningPattern(vararg startWords: String) =
        PlatformPatterns.psiElement(FuncElementTypes.IDENTIFIER).and(onStatementBeginning(*startWords))

    fun macroPattern(): PsiElementPattern.Capture<PsiElement> =
        baseDeclarationPattern().and(identifierStatementBeginningPattern("#"))

    private class OnStatementBeginning(vararg startWords: String) :
        PatternCondition<PsiElement>("onStatementBeginning") {
        private val _startWords = startWords

        override fun accepts(t: PsiElement, context: ProcessingContext?): Boolean {
            val prev = t.prevVisibleOrNewLine
            return if (_startWords.isEmpty())
                prev == null || prev is PsiWhiteSpace
            else
                prev != null && prev.node.text in _startWords
        }
    }

    private class TestPattern() : PatternCondition<PsiElement>("testPattern") {
        override fun accepts(t: PsiElement, context: ProcessingContext?): Boolean {
            println("t: $t - `${t.text}` | parent: ${t.parent}")
            return true
        }
    }
}
