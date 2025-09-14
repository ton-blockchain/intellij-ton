package org.ton.intellij.tolk.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.util.parentOfType
import org.ton.intellij.tolk.TolkIcons
import org.ton.intellij.tolk.doc.psi.TolkDocComment
import org.ton.intellij.tolk.psi.TolkFunction
import org.ton.intellij.tolk.psi.TolkStruct
import org.ton.intellij.tolk.psi.TolkStructField
import org.ton.intellij.tolk.stub.TolkStructFieldStub
import org.ton.intellij.tolk.type.TolkTy
import org.ton.intellij.tolk.type.TolkTyStruct
import org.ton.intellij.util.childOfType
import javax.swing.Icon

abstract class TolkStructFieldMixin : TolkNamedElementImpl<TolkStructFieldStub>, TolkStructField {
    constructor(node: ASTNode) : super(node)

    constructor(stub: TolkStructFieldStub, stubType: IStubElementType<*, *>) : super(stub, stubType)

    override val type: TolkTy?
        get() = typeExpression?.type

    override val doc: TolkDocComment?
        get() = childOfType<TolkDocComment>()

    override fun getIcon(flags: Int): Icon = TolkIcons.FIELD
}

val TolkStructField.parentStruct: TolkStruct
    get() = parentOfType<TolkStruct>()!!

val TolkStructField.isPrivate: Boolean
    get() = structFieldModifiers?.structFieldModifierList?.any { it.text == "private" } == true

val TolkStructField.isReadonly: Boolean
    get() = structFieldModifiers?.structFieldModifierList?.any { it.text == "readonly" } == true

fun TolkStructField.canUse(qualifierType: TolkTyStruct, context: PsiElement): Boolean {
    if (!isPrivate) {
        // non-private fields can be used everywhere
        return true
    }

    val outerMethod = context.parentOfType<TolkFunction>() ?: return false
    if (!outerMethod.hasReceiver) {
        // cannot use private fields in functions
        return false
    }

    val selfType = outerMethod.receiverTy
    if (selfType !is TolkTyStruct) return false

    if (!selfType.isEquivalentTo(this.parentStruct.declaredType)) {
        // cannot use private fields of other structs
        return false
    }

    // can access private fields only in methods of this struct and with qualifier with this struct type
    return qualifierType.isEquivalentTo(this.parentStruct.declaredType)
}
