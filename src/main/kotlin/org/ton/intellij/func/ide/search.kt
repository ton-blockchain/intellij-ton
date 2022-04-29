package org.ton.intellij.func.ide

import com.intellij.lang.HelpID
import com.intellij.lang.cacheBuilder.DefaultWordsScanner
import com.intellij.lang.cacheBuilder.WordsScanner
import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet
import org.ton.intellij.func.lexer.FuncLexerAdapter
import org.ton.intellij.func.psi.FuncNamedElement
import org.ton.intellij.func.psi.FuncTokenTypes

class FuncFindUsagesProvider : FindUsagesProvider {
    override fun getWordsScanner(): WordsScanner = FuncWordScanner()
    override fun canFindUsagesFor(element: PsiElement) = element is FuncNamedElement

    override fun getHelpId(element: PsiElement) = HelpID.FIND_OTHER_USAGES
    override fun getType(element: PsiElement) = ""
    override fun getDescriptiveName(element: PsiElement) = ""
    override fun getNodeText(element: PsiElement, useFullName: Boolean) = ""
}

class FuncWordScanner : DefaultWordsScanner(
    FuncLexerAdapter(),
    TokenSet.create(FuncTokenTypes.IDENTIFIER),
    FuncParserDefinition.COMMENTS,
    TokenSet.create(FuncTokenTypes.STRING_LITERAL)
)
