package org.ton.intellij.fift.ide

import com.intellij.lang.HelpID
import com.intellij.lang.cacheBuilder.DefaultWordsScanner
import com.intellij.lang.cacheBuilder.WordsScanner
import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.psi.ElementDescriptionUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet
import com.intellij.usageView.UsageViewLongNameLocation
import com.intellij.usageView.UsageViewShortNameLocation
import org.ton.intellij.fift.lexer.FiftLexerAdapter
import org.ton.intellij.fift.psi.FiftGlobalVar
import org.ton.intellij.fift.psi.FiftMethodDefinition
import org.ton.intellij.fift.psi.FiftProcDefinition
import org.ton.intellij.fift.psi.FiftProcInlineDefinition
import org.ton.intellij.fift.psi.FiftTypes
import org.ton.intellij.util.tokenSetOf

class FiftAssemblyFindUsagesProvider : FindUsagesProvider {
    override fun getWordsScanner(): WordsScanner = DefaultWordsScanner(
        FiftLexerAdapter(),
        TokenSet.create(FiftTypes.IDENTIFIER),
        FiftParserDefinition.COMMENTS,
        tokenSetOf()
    )

    override fun canFindUsagesFor(psiElement: PsiElement): Boolean = true

    override fun getHelpId(psiElement: PsiElement): String = HelpID.FIND_OTHER_USAGES

    override fun getType(element: PsiElement): String {
        val grand = element.parent.parent
        return when (grand) {
            is FiftGlobalVar                                   -> "global variable"
            is FiftProcDefinition, is FiftProcInlineDefinition -> "procedure"
            is FiftMethodDefinition                            -> "method"
            else                                               -> return ""
        }
    }

    override fun getDescriptiveName(element: PsiElement): String =
        ElementDescriptionUtil.getElementDescription(element, UsageViewLongNameLocation.INSTANCE)

    override fun getNodeText(element: PsiElement, useFullName: Boolean): String =
        ElementDescriptionUtil.getElementDescription(element, UsageViewShortNameLocation.INSTANCE)
}
