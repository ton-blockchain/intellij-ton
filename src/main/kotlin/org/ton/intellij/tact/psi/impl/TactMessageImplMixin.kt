package org.ton.intellij.tact.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.ton.intellij.tact.psi.*
import org.ton.intellij.tact.stub.TactFieldStub
import org.ton.intellij.tact.stub.TactMessageStub
import org.ton.intellij.tact.type.TactTy
import org.ton.intellij.tact.type.TactTyRef
import org.ton.intellij.util.getChildrenByType

abstract class TactMessageImplMixin : TactNamedElementImpl<TactMessageStub>, TactMessage {
    constructor(node: ASTNode) : super(node)

    constructor(stub: TactMessageStub, type: IStubElementType<*, *>) : super(stub, type)

    override val declaredTy: TactTy
        get() = TactTyRef(this)

    val fields: List<TactField>
        get() = CachedValuesManager.getCachedValue(this) {
            val stub = stub
            val functions = if (stub != null) {
                getChildrenByType(stub, TactElementTypes.FIELD, TactFieldStub.Type.ARRAY_FACTORY)
            } else {
                blockFields?.fieldList ?: emptyList()
            }
            CachedValueProvider.Result.create(functions, this)
        }


    override val members: Sequence<TactNamedElement>
        get() = fields.asSequence()

}
