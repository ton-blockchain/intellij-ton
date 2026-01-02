package org.ton.intellij.tolk.ide.completion.postfix

import com.intellij.codeInsight.template.postfix.templates.PostfixTemplate
import com.intellij.codeInsight.template.postfix.templates.PostfixTemplateExpressionSelector
import com.intellij.codeInsight.template.postfix.templates.PostfixTemplateExpressionSelectorBase
import com.intellij.codeInsight.template.postfix.templates.PostfixTemplateProvider
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.Condition
import com.intellij.openapi.util.Conditions
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.SyntaxTraverser
import com.intellij.psi.util.PsiUtilCore
import org.ton.intellij.tolk.psi.TolkExpression
import org.ton.intellij.tolk.psi.TolkStatement

class TolkPostfixTemplateProvider : PostfixTemplateProvider {
    override fun getTemplates(): MutableSet<PostfixTemplate> = hashSetOf(
        TolkVarPostfixTemplate(),
        TolkValPostfixTemplate(),
        TolkIfPostfixTemplate(),
        TolkArgPostfixTemplate(),
        TolkParPostfixTemplate(),
        TolkMatchPostfixTemplate(),
        TolkNotPostfixTemplate(),
        TolkPrintlnPostfixTemplate(),
    )

    override fun isTerminalSymbol(currentChar: Char) = currentChar == '.'
    override fun preExpand(file: PsiFile, editor: Editor) {}
    override fun afterExpand(file: PsiFile, editor: Editor) {}
    override fun preCheck(copyFile: PsiFile, realEditor: Editor, currentOffset: Int) = copyFile
}

fun findAllExpressions(condition: Condition<in PsiElement?>): PostfixTemplateExpressionSelector {
    return object : PostfixTemplateExpressionSelectorBase(condition) {
        override fun getNonFilteredExpressions(context: PsiElement, document: Document, offset: Int): List<PsiElement> {
            val at = PsiUtilCore.getElementAtOffset(context.containingFile, offset - 1)
            return SyntaxTraverser.psiApi().parents(at)
                .takeWhile(Conditions.notInstanceOf(TolkStatement::class.java))
                .filter(TolkExpression::class.java)
                .filter(PsiElement::class.java)
                .toList()
        }
    }
}