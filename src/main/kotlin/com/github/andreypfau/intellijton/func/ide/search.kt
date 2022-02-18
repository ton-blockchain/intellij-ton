package com.github.andreypfau.intellijton.func.ide

import com.github.andreypfau.intellijton.func.lexer.FuncLexerAdapter
import com.github.andreypfau.intellijton.func.psi.FuncNamedElement
import com.github.andreypfau.intellijton.func.psi.FuncTypes
import com.intellij.lang.HelpID
import com.intellij.lang.cacheBuilder.DefaultWordsScanner
import com.intellij.lang.cacheBuilder.WordsScanner
import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet

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
    TokenSet.create(FuncTypes.IDENTIFIER),
    FuncParserDefinition.COMMENTS,
    TokenSet.create(FuncTypes.STRING_LITERAL)
)
