package org.ton.intellij.tact.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType
import org.ton.intellij.tact.psi.TactFunction
import org.ton.intellij.tact.psi.TactInferenceContextOwner
import org.ton.intellij.tact.psi.TactNamedElementImpl
import org.ton.intellij.tact.stub.TactFunctionStub
import org.ton.intellij.util.greenStub

abstract class TactFunctionImplMixin : TactNamedElementImpl<TactFunctionStub>, TactFunction, TactInferenceContextOwner {
    constructor(node: ASTNode) : super(node)

    constructor(stub: TactFunctionStub, type: IStubElementType<*, *>) : super(stub, type)

    override val body get() = block
}

val TactFunction.isNative get() = greenStub?.isNative ?: (nativeKeyword != null)
val TactFunction.isGet get() = greenStub?.isGet ?: functionAttributeList.any { it.getKeyword != null }
val TactFunction.isMutates get() = greenStub?.isMutates ?: functionAttributeList.any { it.mutatesKeyword != null }
val TactFunction.isExtends get() = greenStub?.isExtends ?: functionAttributeList.any { it.extendsKeyword != null }
val TactFunction.isVirtual get() = greenStub?.isVirtual ?: functionAttributeList.any { it.virtualKeyword != null }
val TactFunction.isOverride get() = greenStub?.isOverride ?: functionAttributeList.any { it.overrideKeyword != null }
val TactFunction.isInline get() = greenStub?.isInline ?: functionAttributeList.any { it.inlineKeyword != null }
val TactFunction.isAbstract get() = greenStub?.isAbstract ?: functionAttributeList.any { it.abstractKeyword != null }
