package org.ton.intellij.func.ide

import com.intellij.lang.HelpID
import com.intellij.lang.cacheBuilder.DefaultWordsScanner
import com.intellij.lang.cacheBuilder.WordsScanner
import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.psi.ElementDescriptionUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet
import com.intellij.usageView.UsageViewLongNameLocation
import com.intellij.usageView.UsageViewShortNameLocation
import org.ton.intellij.func.lexer.FuncLexer
import org.ton.intellij.func.parser.FuncParserDefinition
import org.ton.intellij.func.psi.*

class FuncFindUsagesProvider : FindUsagesProvider {
    override fun getWordsScanner(): WordsScanner = DefaultWordsScanner(
        FuncLexer(),
        TokenSet.create(FuncElementTypes.IDENTIFIER),
        FUNC_COMMENTS,
        FuncParserDefinition.STRING_LITERALS
    )

    override fun canFindUsagesFor(psiElement: PsiElement): Boolean {
        return true
    }

    override fun getHelpId(psiElement: PsiElement): String = HelpID.FIND_OTHER_USAGES

    override fun getType(element: PsiElement): String {
        return when (element) {
            is FuncFunction -> "function"
            is FuncConstVar -> "constant"
            is FuncGlobalVar -> "global var"
            is FuncTypeParameter -> "type parameter"
            else -> return "<TYPE $element>"
        }
    }

    override fun getDescriptiveName(element: PsiElement): String =
        ElementDescriptionUtil.getElementDescription(element, UsageViewLongNameLocation.INSTANCE)

    override fun getNodeText(element: PsiElement, useFullName: Boolean): String =
        ElementDescriptionUtil.getElementDescription(element, UsageViewShortNameLocation.INSTANCE)
}
