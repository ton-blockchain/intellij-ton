package com.github.andreypfau.intellijton.fift.ide

import com.github.andreypfau.intellijton.fift.core.FiftParserDefinition
import com.github.andreypfau.intellijton.fift.psi.FiftNamedElement
import com.github.andreypfau.intellijton.fift.psi.FiftTypes
import com.github.andreypfau.intellijton.func.parser.FuncLexerAdapter
import com.intellij.lang.HelpID
import com.intellij.lang.cacheBuilder.DefaultWordsScanner
import com.intellij.lang.cacheBuilder.WordsScanner
import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet

class FiftFindUsagesProvider : FindUsagesProvider {
    override fun getWordsScanner(): WordsScanner = FiftWordScanner()
    override fun canFindUsagesFor(element: PsiElement) = element is FiftNamedElement

    override fun getHelpId(element: PsiElement) = HelpID.FIND_OTHER_USAGES
    override fun getType(element: PsiElement) = ""
    override fun getDescriptiveName(element: PsiElement) = ""
    override fun getNodeText(element: PsiElement, useFullName: Boolean) = ""
}

class FiftWordScanner : DefaultWordsScanner(
    FuncLexerAdapter(),
    TokenSet.create(FiftTypes.IDENTIFIER),
    FiftParserDefinition.COMMENTS,
    TokenSet.create(FiftTypes.STRING_LITERAL)
)
