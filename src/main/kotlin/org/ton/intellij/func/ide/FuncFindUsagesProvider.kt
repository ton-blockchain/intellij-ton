package org.ton.intellij.func.ide

import com.intellij.lang.cacheBuilder.DefaultWordsScanner
import com.intellij.lang.cacheBuilder.WordsScanner
import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.tree.TokenSet
import org.ton.intellij.func.lexer.FuncLexer
import org.ton.intellij.func.parser.FuncParserDefinition
import org.ton.intellij.func.psi.FuncElementTypes
import org.ton.intellij.func.psi.FuncFunction

class FuncFindUsagesProvider : FindUsagesProvider {
    override fun getWordsScanner(): WordsScanner = DefaultWordsScanner(
        FuncLexer(),
        TokenSet.create(FuncElementTypes.IDENTIFIER),
        FuncParserDefinition.COMMENTS,
        FuncParserDefinition.STRING_LITERALS
    )

    override fun canFindUsagesFor(psiElement: PsiElement): Boolean {
        println("canFindUsagesFor: $psiElement")
        return true
    }

    override fun getHelpId(psiElement: PsiElement): String? {
        return null
    }

    override fun getType(element: PsiElement): String {
        if (element is FuncFunction) {
            return "function"
        }
        return ""
    }

    override fun getDescriptiveName(element: PsiElement): String {
        if (element is PsiNamedElement) {
            return element.name ?: ""
        }
        return ""
    }

    override fun getNodeText(element: PsiElement, useFullName: Boolean): String {
        if (element is PsiNamedElement) {
            return element.name ?: ""
        }
        return ""
    }
}
