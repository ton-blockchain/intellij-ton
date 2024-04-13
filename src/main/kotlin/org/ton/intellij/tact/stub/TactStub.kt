package org.ton.intellij.tact.stub

import com.intellij.psi.PsiFile
import com.intellij.psi.StubBuilder
import com.intellij.psi.stubs.*
import com.intellij.psi.tree.IStubFileElementType
import com.intellij.util.BitUtil
import com.intellij.util.io.StringRef
import org.ton.intellij.tact.TactLanguage
import org.ton.intellij.tact.psi.*
import org.ton.intellij.tact.psi.impl.*
import org.ton.intellij.tact.stub.index.indexFunction
import org.ton.intellij.util.BitFlagsBuilder

abstract class TactNamedStub<T : TactNamedElement>(
    parent: StubElement<*>?,
    elementType: IStubElementType<*, *>,
    name: StringRef?,
) : NamedStubBase<T>(parent, elementType, name) {
    constructor(parent: StubElement<*>, elementType: IStubElementType<*, *>, name: String?) : this(
        parent,
        elementType,
        StringRef.fromString(name)
    )

    override fun toString(): String {
        return "${javaClass.simpleName}($name)"
    }
}

abstract class TactStubElementType<StubT : StubElement<*>, PsiT : TactElement>(
    debugName: String
) : IStubElementType<StubT, PsiT>(debugName, TactLanguage) {

    final override fun getExternalId(): String = "tact.${super.toString()}"

    override fun indexStub(stub: StubT, sink: IndexSink) {}
}

class TactFileStub(
    file: TactFile?,
) : PsiFileStubImpl<TactFile>(file) {
    override fun getType() = Type

    object Type : IStubFileElementType<TactFileStub>(TactLanguage) {
        private const val STUB_VERSION = 3

        override fun getStubVersion(): Int = STUB_VERSION

        override fun getBuilder(): StubBuilder = object : DefaultStubBuilder() {
            override fun createStubForFile(file: PsiFile): StubElement<*> =
                if (file is TactFile) TactFileStub(file)
                else super.createStubForFile(file)
        }

        override fun indexStub(stub: PsiFileStub<*>, sink: IndexSink) {
            super.indexStub(stub, sink)
        }

        override fun serialize(stub: TactFileStub, dataStream: StubOutputStream) {

        }

        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): TactFileStub {
            return TactFileStub(null)
        }

        override fun getExternalId(): String = "tact.FILE"
    }
}

fun factory(name: String): TactStubElementType<*, *> {
    return when (name) {
        "FUNCTION" -> TactFunctionStub.Type
        "MESSAGE" -> TactMessageStub.Type
        "STRUCT" -> TactStructStub.Type
        "TRAIT" -> TactTraitStub.Type
        "CONTRACT" -> TactContractStub.Type
        "PRIMITIVE" -> TactPrimitiveStub.Type
        "FIELD" -> TactFieldStub.Type
        else -> error("Unknown element type: $name")
    }
}
