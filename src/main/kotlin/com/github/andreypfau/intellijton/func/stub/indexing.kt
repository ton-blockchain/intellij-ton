package com.github.andreypfau.intellijton.func.stub

import com.intellij.psi.stubs.IndexSink

fun IndexSink.indexFunctionDef(stub: FuncFunctionStub) {
    indexNamedStub(stub)
    indexFunction(stub)
}

private fun IndexSink.indexNamedStub(stub: FuncNamedStub) = stub.name?.let {
    occurrence(FuncNamedElementIndex.KEY, it)
}

private fun IndexSink.indexFunction(stub: FuncFunctionStub) = stub.name?.let {
    occurrence(FuncFunctionIndex.KEY, it)
}
