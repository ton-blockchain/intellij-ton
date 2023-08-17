package org.ton.intellij.fift.ide

import com.intellij.lang.HelpID
import com.intellij.lang.cacheBuilder.DefaultWordsScanner
import com.intellij.lang.cacheBuilder.WordsScanner
import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet
import org.ton.intellij.fift.lexer.FiftLexerAdapter
import org.ton.intellij.fift.psi.FiftNamedElement
import org.ton.intellij.fift.psi.FiftTypes

class FiftFindUsagesProvider : FindUsagesProvider {
    override fun getWordsScanner(): WordsScanner = FiftWordScanner()
    override fun canFindUsagesFor(element: PsiElement) = element is FiftNamedElement

    override fun getHelpId(element: PsiElement) = HelpID.FIND_OTHER_USAGES
    override fun getType(element: PsiElement) = ""
    override fun getDescriptiveName(element: PsiElement) = ""
    override fun getNodeText(element: PsiElement, useFullName: Boolean) = ""
}

class FiftWordScanner : DefaultWordsScanner(
    FiftLexerAdapter(),
    TokenSet.create(FiftTypes.IDENTIFIER),
    FiftParserDefinition.COMMENTS,
    TokenSet.create(FiftTypes.STRING_LITERAL)
)
