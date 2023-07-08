package org.ton.intellij.func.stub.type

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.ArrayFactory
import org.ton.intellij.func.psi.FuncIncludeDefinition
import org.ton.intellij.func.psi.impl.FuncIncludeDefinitionImpl
import org.ton.intellij.func.stub.FuncIncludeDefinitionStub

class FuncIncludeDefinitionStubElementType(
    debugName: String,
) : FuncStubElementType<FuncIncludeDefinitionStub, FuncIncludeDefinition>(
    debugName
) {
    override fun serialize(stub: FuncIncludeDefinitionStub, dataStream: StubOutputStream) {
        dataStream.writeUTFFast(stub.path)
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>): FuncIncludeDefinitionStub {
        val path = dataStream.readUTFFast()
        return FuncIncludeDefinitionStub(parentStub, this, path)
    }

    override fun createStub(
        psi: FuncIncludeDefinition,
        parentStub: StubElement<out PsiElement>,
    ): FuncIncludeDefinitionStub {
        return FuncIncludeDefinitionStub(parentStub, this, psi.includePath.stringLiteral.rawString.text)
    }

    override fun createPsi(stub: FuncIncludeDefinitionStub): FuncIncludeDefinition {
        return FuncIncludeDefinitionImpl(stub, this)
    }

    companion object {
        val EMPTY_ARRAY = emptyArray<FuncIncludeDefinition>()
        val ARRAY_FACTORY: ArrayFactory<FuncIncludeDefinition?> = ArrayFactory {
            if (it == 0) EMPTY_ARRAY else arrayOfNulls(it)
        }
    }
}
