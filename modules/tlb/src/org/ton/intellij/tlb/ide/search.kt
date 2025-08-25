package org.ton.intellij.tlb.ide

import com.intellij.lang.HelpID
import com.intellij.lang.cacheBuilder.DefaultWordsScanner
import com.intellij.lang.cacheBuilder.WordsScanner
import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.psi.ElementDescriptionUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet
import com.intellij.usageView.UsageViewLongNameLocation
import com.intellij.usageView.UsageViewShortNameLocation
import org.ton.intellij.tlb.lexer.TlbLexerAdapter
import org.ton.intellij.tlb.psi.TlbField
import org.ton.intellij.tlb.psi.TlbImplicitField
import org.ton.intellij.tlb.psi.TlbNamedElement
import org.ton.intellij.tlb.psi.TlbResultType
import org.ton.intellij.tlb.psi.TlbTypes

class TlbFindUsagesProvider : FindUsagesProvider {
    override fun getWordsScanner(): WordsScanner = TlbWordScanner()
    override fun canFindUsagesFor(element: PsiElement) = element is TlbNamedElement

    override fun getHelpId(element: PsiElement) = HelpID.FIND_OTHER_USAGES

    override fun getType(element: PsiElement) = when (element) {
        is TlbResultType    -> "Type"
        is TlbImplicitField -> "Implicit Field"
        is TlbField         -> "Field"
        else                -> ""
    }

    override fun getDescriptiveName(element: PsiElement) =
        ElementDescriptionUtil.getElementDescription(element, UsageViewLongNameLocation.INSTANCE)

    override fun getNodeText(element: PsiElement, useFullName: Boolean) =
        ElementDescriptionUtil.getElementDescription(element, UsageViewShortNameLocation.INSTANCE)
}

class TlbWordScanner : DefaultWordsScanner(
    TlbLexerAdapter(),
    TokenSet.create(TlbTypes.IDENTIFIER),
    TlbParserDefinition.COMMENTS,
    TokenSet.EMPTY
)
