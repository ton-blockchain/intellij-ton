package org.ton.intellij.tolk.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import org.ton.intellij.tolk.TolkIcons
import org.ton.intellij.tolk.doc.psi.TolkDocComment
import org.ton.intellij.tolk.psi.TolkStruct
import org.ton.intellij.tolk.psi.TolkStructField
import org.ton.intellij.tolk.stub.TolkStructStub
import org.ton.intellij.tolk.type.TolkTy
import org.ton.intellij.tolk.type.TolkTyStruct
import org.ton.intellij.util.childOfType
import javax.swing.Icon

abstract class TolkStructMixin : TolkNamedElementImpl<TolkStructStub>, TolkStruct {
    constructor(node: ASTNode) : super(node)

    constructor(stub: TolkStructStub, stubType: IStubElementType<*, *>) : super(stub, stubType)

    override val type: TolkTyStruct get() = declaredType

    override val doc: TolkDocComment? get() = childOfType()

    override fun getIcon(flags: Int): Icon? = TolkIcons.STRUCTURE

    override fun isEquivalentTo(another: PsiElement?): Boolean {
        if (this === another) return true
        if (another !is TolkStruct) return false
        if (name != another.name) return false
        if (originalElement == another.originalElement) return true
        val thisFile = containingFile.originalFile
        val anotherFile = another.containingFile.originalFile
        return thisFile == anotherFile
    }
}

val TolkStruct.declaredType: TolkTyStruct get() = TolkTy.struct(this)

val TolkStruct?.structFields: List<TolkStructField>
    get() {
        // todo: optimize by stub
        return this?.structBody?.structFieldList ?: emptyList()
    }
