package org.ton.intellij.tlb.ide

import com.intellij.lang.HelpID
import com.intellij.lang.cacheBuilder.DefaultWordsScanner
import com.intellij.lang.cacheBuilder.WordsScanner
import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet
import org.ton.intellij.tlb.lexer.TlbLexerAdapter
import org.ton.intellij.tlb.psi.TlbNamedElement
import org.ton.intellij.tlb.psi.TlbTypes

class TlbFindUsagesProvider : FindUsagesProvider {
    override fun getWordsScanner(): WordsScanner = TlbWordScanner()
    override fun canFindUsagesFor(element: PsiElement) = element is TlbNamedElement

    override fun getHelpId(element: PsiElement) = HelpID.FIND_OTHER_USAGES
    override fun getType(element: PsiElement) = ""
    override fun getDescriptiveName(element: PsiElement) = ""
    override fun getNodeText(element: PsiElement, useFullName: Boolean) = ""
}

class TlbWordScanner : DefaultWordsScanner(
    TlbLexerAdapter(),
    TokenSet.create(TlbTypes.IDENTIFIER),
    TlbParserDefinition.COMMENTS,
    TokenSet.EMPTY
)
