package org.ton.intellij.tolk.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType
import org.ton.intellij.tolk.TolkIcons
import org.ton.intellij.tolk.doc.psi.TolkDocComment
import org.ton.intellij.tolk.psi.TolkStruct
import org.ton.intellij.tolk.psi.TolkStructField
import org.ton.intellij.tolk.stub.TolkStructFieldStub
import org.ton.intellij.tolk.type.TolkTy
import org.ton.intellij.util.childOfType
import org.ton.intellij.util.parentOfType
import javax.swing.Icon

abstract class TolkStructFieldMixin : TolkNamedElementImpl<TolkStructFieldStub>, TolkStructField {
    constructor(node: ASTNode) : super(node)

    constructor(stub: TolkStructFieldStub, stubType: IStubElementType<*, *>) : super(stub, stubType)

    override val type: TolkTy?
        get() = typeExpression?.type

    override val doc: TolkDocComment?
        get() = childOfType<TolkDocComment>()

    override fun getIcon(flags: Int): Icon = TolkIcons.FIELD

    val isPrivate: Boolean
        get() = structFieldModifiers?.structFieldModifierList?.any { it.text == "private" } == true

    val isReadonly: Boolean
        get() = structFieldModifiers?.structFieldModifierList?.any { it.text == "readonly" } == true
}

val TolkStructField.parentStruct: TolkStruct
    get() = parentOfType<TolkStruct>()!!
