package org.ton.intellij.tolk.ide

import com.intellij.lang.HelpID
import com.intellij.lang.cacheBuilder.DefaultWordsScanner
import com.intellij.lang.cacheBuilder.WordsScanner
import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.psi.ElementDescriptionUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet
import com.intellij.usageView.UsageViewLongNameLocation
import com.intellij.usageView.UsageViewShortNameLocation
import org.ton.intellij.tolk.lexer.TolkLexer
import org.ton.intellij.tolk.parser.TolkParserDefinition
import org.ton.intellij.tolk.psi.*

class TolkFindUsagesProvider : FindUsagesProvider {
    override fun getWordsScanner(): WordsScanner = DefaultWordsScanner(
        TolkLexer(),
        TokenSet.create(TolkElementTypes.IDENTIFIER),
        TOLK_COMMENTS,
        TolkParserDefinition.STRING_LITERALS
    )

    override fun canFindUsagesFor(psiElement: PsiElement): Boolean {
        return true
    }

    override fun getHelpId(psiElement: PsiElement): String = HelpID.FIND_OTHER_USAGES

    override fun getType(element: PsiElement): String {
        return when (element) {
            is TolkFunction -> "function"
            is TolkConstVar -> "constant"
            is TolkGlobalVar -> "global var"
            is TolkTypeParameter -> "type parameter"
            else -> return "<TYPE $element>"
        }
    }

    override fun getDescriptiveName(element: PsiElement): String =
        ElementDescriptionUtil.getElementDescription(element, UsageViewLongNameLocation.INSTANCE)

    override fun getNodeText(element: PsiElement, useFullName: Boolean): String =
        ElementDescriptionUtil.getElementDescription(element, UsageViewShortNameLocation.INSTANCE)
}
