package org.ton.intellij.tolk.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType
import org.ton.intellij.tolk.TolkIcons
import org.ton.intellij.tolk.doc.psi.TolkDocComment
import org.ton.intellij.tolk.psi.TolkEnum
import org.ton.intellij.tolk.psi.TolkStruct
import org.ton.intellij.tolk.psi.TolkEnumMember
import org.ton.intellij.tolk.stub.TolkEnumMemberStub
import org.ton.intellij.tolk.type.TolkTy
import org.ton.intellij.util.childOfType
import org.ton.intellij.util.parentOfType
import javax.swing.Icon

abstract class TolkEnumMemberMixin : TolkNamedElementImpl<TolkEnumMemberStub>, TolkEnumMember {
    constructor(node: ASTNode) : super(node)

    constructor(stub: TolkEnumMemberStub, stubType: IStubElementType<*, *>) : super(stub, stubType)

    override val type: TolkTy?
        get() = parentOfType<TolkEnum>()?.declaredType

    override val doc: TolkDocComment?
        get() = childOfType<TolkDocComment>()

    override fun getIcon(flags: Int): Icon = TolkIcons.FIELD
}

val TolkEnumMember.parentEnum: TolkEnum
    get() = parentOfType<TolkEnum>()!!
