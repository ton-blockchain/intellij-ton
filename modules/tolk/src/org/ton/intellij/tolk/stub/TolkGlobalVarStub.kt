package org.ton.intellij.tolk.stub

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.ArrayFactory
import com.intellij.util.io.StringRef
import org.ton.intellij.tolk.psi.TolkGlobalVar
import org.ton.intellij.tolk.psi.impl.TolkGlobalVarImpl
import org.ton.intellij.tolk.stub.type.TolkNamedStubElementType

class TolkGlobalVarStub(
    parent: StubElement<*>,
    elementType: IStubElementType<*, *>,
    name: StringRef?,
    val isDeprecated: Boolean,
) : TolkNamedStub<TolkGlobalVar>(parent, elementType, name) {
    object Type : TolkNamedStubElementType<TolkGlobalVarStub, TolkGlobalVar>("GLOBAL_VAR") {
        override fun serialize(stub: TolkGlobalVarStub, dataStream: StubOutputStream) {
            dataStream.writeName(stub.name)
            dataStream.writeBoolean(stub.isDeprecated)
        }

        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>): TolkGlobalVarStub {
            val name = dataStream.readName()
            val isDeprecated = dataStream.readBoolean()
            return TolkGlobalVarStub(parentStub, this, name, isDeprecated)
        }

        override fun createStub(psi: TolkGlobalVar, parentStub: StubElement<out PsiElement>): TolkGlobalVarStub =
            TolkGlobalVarStub(parentStub, this, StringRef.fromString(psi.name), psi.annotations.hasDeprecatedAnnotation())

        override fun createPsi(stub: TolkGlobalVarStub): TolkGlobalVar =
            TolkGlobalVarImpl(stub, this)
    }

    companion object {
        val EMPTY_ARRAY = emptyArray<TolkGlobalVar>()
        val ARRAY_FACTORY: ArrayFactory<TolkGlobalVar?> = ArrayFactory {
            if (it == 0) EMPTY_ARRAY else arrayOfNulls(it)
        }
    }
}
