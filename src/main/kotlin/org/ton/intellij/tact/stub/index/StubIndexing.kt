package org.ton.intellij.tact.stub.index

import com.intellij.psi.stubs.IndexSink
import org.ton.intellij.tact.stub.*

fun IndexSink.indexFunction(stub: TactFunctionStub) {
    stub.name?.let {
        occurrence(TactNamedElementIndex.KEY, it)
        occurrence(TactFunctionIndex.KEY, it)
    }
}

fun IndexSink.indexMessage(stub: TactMessageStub) {
    stub.name?.let {
        occurrence(TactNamedElementIndex.KEY, it)
        occurrence(TactTypesIndex.KEY, it)
    }
}

fun IndexSink.indexStruct(stub: TactStructStub) {
    stub.name?.let {
        occurrence(TactNamedElementIndex.KEY, it)
        occurrence(TactTypesIndex.KEY, it)
    }
}

fun IndexSink.indexTrait(stub: TactTraitStub) {
    stub.name?.let {
        occurrence(TactNamedElementIndex.KEY, it)
        occurrence(TactTypesIndex.KEY, it)
    }
}

fun IndexSink.indexContract(stub: TactContractStub) {
    stub.name?.let {
        occurrence(TactNamedElementIndex.KEY, it)
        occurrence(TactTypesIndex.KEY, it)
    }
}

fun IndexSink.indexPrimitive(stub: TactPrimitiveStub) {
    stub.name?.let {
        occurrence(TactNamedElementIndex.KEY, it)
        occurrence(TactTypesIndex.KEY, it)
    }
}

fun IndexSink.indexField(stub: TactFieldStub) {
    stub.name?.let {
        occurrence(TactNamedElementIndex.KEY, it)
    }
}

fun IndexSink.indexConstant(stub: TactConstantStub) {
    stub.name?.let {
        occurrence(TactNamedElementIndex.KEY, it)
        occurrence(TactConstantIndex.KEY, it)
    }
}
