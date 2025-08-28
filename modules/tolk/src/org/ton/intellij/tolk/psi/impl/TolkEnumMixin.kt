package org.ton.intellij.tolk.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import org.ton.intellij.tolk.TolkIcons
import org.ton.intellij.tolk.doc.psi.TolkDocComment
import org.ton.intellij.tolk.psi.TolkEnum
import org.ton.intellij.tolk.psi.TolkEnumMember
import org.ton.intellij.tolk.stub.TolkEnumStub
import org.ton.intellij.tolk.type.TolkTyEnum
import org.ton.intellij.util.childOfType
import javax.swing.Icon

abstract class TolkEnumMixin : TolkNamedElementImpl<TolkEnumStub>, TolkEnum {
    constructor(node: ASTNode) : super(node)

    constructor(stub: TolkEnumStub, stubType: IStubElementType<*, *>) : super(stub, stubType)

    override val type: TolkTyEnum get() = declaredType

    override val doc: TolkDocComment? get() = childOfType()

    override fun getIcon(flags: Int): Icon? = TolkIcons.ENUM

    override fun isEquivalentTo(another: PsiElement?): Boolean {
        if (this === another) return true
        if (another !is TolkEnum) return false
        if (name != another.name) return false
        if (originalElement == another.originalElement) return true
        val thisFile = containingFile.originalFile
        val anotherFile = another.containingFile.originalFile
        return thisFile == anotherFile
    }
}

val TolkEnum.declaredType: TolkTyEnum get() = TolkTyEnum.create(this)

val TolkEnum?.members: List<TolkEnumMember>
    get() {
        // todo: optimize by stub
        return this?.enumBody?.enumMemberList ?: emptyList()
    }
