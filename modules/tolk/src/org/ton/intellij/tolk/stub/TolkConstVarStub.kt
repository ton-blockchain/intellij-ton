package org.ton.intellij.tolk.stub

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.ArrayFactory
import com.intellij.util.io.StringRef
import org.ton.intellij.tolk.psi.TolkConstVar
import org.ton.intellij.tolk.psi.impl.TolkConstVarImpl
import org.ton.intellij.tolk.stub.type.TolkNamedStubElementType

class TolkConstVarStub(
    parent: StubElement<*>,
    elementType: IStubElementType<*, *>,
    name: StringRef?,
    val isDeprecated: Boolean
) : TolkNamedStub<TolkConstVar>(parent, elementType, name) {
    object Type : TolkNamedStubElementType<TolkConstVarStub, TolkConstVar>("CONST_VAR") {
        override fun serialize(stub: TolkConstVarStub, dataStream: StubOutputStream) {
            dataStream.writeName(stub.name)
            dataStream.writeBoolean(stub.isDeprecated)
        }

        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>): TolkConstVarStub {
            val name = dataStream.readName()
            val isDeprecated = dataStream.readBoolean()
            return TolkConstVarStub(parentStub, this, name, isDeprecated)
        }

        override fun createStub(psi: TolkConstVar, parentStub: StubElement<out PsiElement>, ): TolkConstVarStub =
            TolkConstVarStub(parentStub, this, StringRef.fromString(psi.name), psi.annotations.hasDeprecatedAnnotation())

        override fun createPsi(stub: TolkConstVarStub): TolkConstVar =
            TolkConstVarImpl(stub, this)
    }

    companion object {
        val EMPTY_ARRAY = emptyArray<TolkConstVar>()
        val ARRAY_FACTORY: ArrayFactory<TolkConstVar> = ArrayFactory {
            if (it == 0) EMPTY_ARRAY else arrayOfNulls<TolkConstVar>(it)
        }
    }
}
