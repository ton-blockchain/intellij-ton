package com.github.andreypfau.intellijton.func.stub

import com.intellij.psi.stubs.IndexSink

fun IndexSink.indexFunctionDef(stub: FuncFunctionDefinitionStub) {
    indexNamedStub(stub)
    indexFunction(stub)
}

private fun IndexSink.indexNamedStub(stub: FuncNamedStub) = stub.name?.let {
    occurrence(FuncNamedElementIndex.KEY, it)
}

private fun IndexSink.indexFunction(stub: FuncFunctionDefinitionStub) = stub.name?.let {
    occurrence(FuncFunctionIndex.KEY, it)
}
