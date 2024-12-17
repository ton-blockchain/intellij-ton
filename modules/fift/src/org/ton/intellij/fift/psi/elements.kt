package org.ton.intellij.fift.psi

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.ide.projectView.PresentationData
import com.intellij.lang.ASTNode
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import org.ton.intellij.fift.FiftIcons
import org.ton.intellij.fift.resolve.FiftOrdinaryWordReference
import javax.swing.Icon

interface FiftElement : PsiElement
abstract class FiftElementImpl(node: ASTNode) : ASTWrapperPsiElement(node), FiftElement
interface FiftNamedElement : FiftElement, PsiNameIdentifierOwner

abstract class FiftNamedElementImpl(node: ASTNode) : FiftElementImpl(node), FiftNamedElement {
    override fun getNameIdentifier(): PsiElement? = findChildByType(FiftTypes.IDENTIFIER)
    override fun getName(): String? = nameIdentifier?.text
    override fun setName(name: String): PsiElement = apply {
        val newWord = project.fiftPsiFactory.createFromText<FiftOrdinaryWord>(name) ?: return this
        replace(newWord)
        return newWord
    }

    override fun getTextOffset(): Int = nameIdentifier?.textOffset ?: super.getTextOffset()
}

abstract class FiftWordDefStatementMixin(node: ASTNode) : FiftNamedElementImpl(node), FiftWordDefStatement {
    override fun getNameIdentifier(): PsiElement? = findChildByType(FiftTypes.WORD_DEF)
    override fun getName(): String? = nameIdentifier?.text?.split(" ")?.let { wordDefText ->
        if (wordDefText.size == 2) {
            wordDefText[1]
        } else null
    }

    override fun setName(name: String): PsiElement = apply {
        val wordDefStatement = requireNotNull(
            project.fiftPsiFactory.createFromText<FiftWordDefStatement>("{ } : $name")
        )
        val newWordDef = wordDefStatement.wordDef
        nameIdentifier?.replace(newWordDef)
    }

    override fun getIcon(flags: Int): Icon = FiftIcons.FUNCTION
    override fun getPresentation(): PresentationData {
        val name = name
        return PresentationData(
            name,
            null,
            FiftIcons.FUNCTION,
            TextAttributesKey.createTextAttributesKey("public")
        )
    }
}

abstract class FiftOrdinaryWordMixin(node: ASTNode) : FiftNamedElementImpl(node), FiftOrdinaryWord {
    override fun getReference() = FiftOrdinaryWordReference(this)
}