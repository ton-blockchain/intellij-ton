package org.ton.intellij.acton.toml

import com.intellij.lang.cacheBuilder.DefaultWordsScanner
import com.intellij.lang.cacheBuilder.WordsScanner
import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet
import org.toml.lang.lexer.TomlLexer
import org.toml.lang.psi.TomlElementTypes
import org.toml.lang.psi.TomlKeySegment

class ActonTomlFindUsagesProvider : FindUsagesProvider {
    override fun getWordsScanner(): WordsScanner = DefaultWordsScanner(
        TomlLexer(),
        TokenSet.create(TomlElementTypes.BARE_KEY),
        TokenSet.EMPTY,
        TokenSet.EMPTY
    )

    override fun canFindUsagesFor(psiElement: PsiElement): Boolean {
        return psiElement is TomlKeySegment
    }

    override fun getHelpId(psiElement: PsiElement): String? = null

    override fun getType(element: PsiElement): String {
        return "Acton element"
    }

    override fun getDescriptiveName(element: PsiElement): String {
        return (element as? TomlKeySegment)?.name ?: ""
    }

    override fun getNodeText(element: PsiElement, useFullName: Boolean): String {
        return getDescriptiveName(element)
    }
}
