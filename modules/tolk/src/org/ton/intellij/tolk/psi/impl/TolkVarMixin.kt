package org.ton.intellij.tolk.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.ton.intellij.tolk.TolkIcons
import org.ton.intellij.tolk.psi.TolkPsiFactory
import org.ton.intellij.tolk.psi.TolkVar
import org.ton.intellij.tolk.psi.TolkVarExpression
import org.ton.intellij.tolk.type.TolkTy
import org.ton.intellij.tolk.type.inference
import org.ton.intellij.util.parentOfType
import javax.swing.Icon

abstract class TolkVarMixin(node: ASTNode) : ASTWrapperPsiElement(node), TolkVar {
    override fun getName(): String = identifier.text.removeSurrounding("`")

    override val rawName: String? = identifier.text

    override fun setName(name: String): PsiElement {
        identifier.replace(TolkPsiFactory[project].createIdentifier(name))
        return this
    }

    override fun getNameIdentifier(): PsiElement = identifier

    override fun getTextOffset(): Int = identifier.textOffset

    override fun getIcon(flags: Int): Icon = TolkIcons.VARIABLE

    override val type: TolkTy?
        get() = typeExpression?.type ?: inference?.getType(this)

    override fun toString(): String = "TolkVar($text)"
}

val TolkVar.isMutable: Boolean
    get() = parentOfType<TolkVarExpression>()?.varKeyword != null

