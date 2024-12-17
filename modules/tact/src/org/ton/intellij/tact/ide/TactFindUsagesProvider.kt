package org.ton.intellij.tact.ide

import com.intellij.lang.HelpID
import com.intellij.lang.cacheBuilder.DefaultWordsScanner
import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.psi.ElementDescriptionUtil
import com.intellij.psi.PsiElement
import com.intellij.usageView.UsageViewLongNameLocation
import com.intellij.usageView.UsageViewShortNameLocation
import org.ton.intellij.tact.lexer.TactLexer
import org.ton.intellij.tact.psi.TACT_COMMENTS
import org.ton.intellij.tact.psi.TactElementTypes
import org.ton.intellij.util.tokenSetOf

class TactFindUsagesProvider : FindUsagesProvider {
    override fun getWordsScanner() = DefaultWordsScanner(
        TactLexer(),
        tokenSetOf(TactElementTypes.IDENTIFIER),
        TACT_COMMENTS,
        tokenSetOf(TactElementTypes.STRING_LITERAL)
    )

    override fun canFindUsagesFor(psiElement: PsiElement): Boolean {
        return true
    }

    override fun getHelpId(psiElement: PsiElement): String? {
        return HelpID.FIND_OTHER_USAGES
    }

    override fun getType(element: PsiElement): String {
        return element.toString()
    }

    override fun getDescriptiveName(element: PsiElement): String =
        ElementDescriptionUtil.getElementDescription(element, UsageViewLongNameLocation.INSTANCE)

    override fun getNodeText(element: PsiElement, useFullName: Boolean): String =
        ElementDescriptionUtil.getElementDescription(element, UsageViewShortNameLocation.INSTANCE)
}
