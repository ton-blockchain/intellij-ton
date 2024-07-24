package org.ton.intellij.tlb.ide

import com.intellij.lang.HelpID
import com.intellij.lang.cacheBuilder.DefaultWordsScanner
import com.intellij.lang.cacheBuilder.WordsScanner
import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet
import org.ton.intellij.tlb.lexer.TlbLexerAdapter
import org.ton.intellij.tlb.psi.TlbConstructor
import org.ton.intellij.tlb.psi.TlbElementTypes
import org.ton.intellij.tlb.psi.TlbNamedElement

class TlbFindUsagesProvider : FindUsagesProvider {
    override fun getWordsScanner(): WordsScanner = TlbWordScanner()
    override fun canFindUsagesFor(element: PsiElement): Boolean {
        return element is TlbNamedElement && element !is TlbConstructor
    }

    override fun getHelpId(element: PsiElement) = HelpID.FIND_OTHER_USAGES
    override fun getType(element: PsiElement) = ""
    override fun getDescriptiveName(element: PsiElement) = ""
    override fun getNodeText(element: PsiElement, useFullName: Boolean) = ""
}

class TlbWordScanner : DefaultWordsScanner(
    TlbLexerAdapter(),
    TokenSet.create(TlbElementTypes.IDENTIFIER),
    TlbParserDefinition.COMMENTS,
    TokenSet.EMPTY
)
