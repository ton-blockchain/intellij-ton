package org.ton.intellij.func.stub

import com.intellij.psi.stubs.IndexSink

fun IndexSink.indexFunctionDef(stub: FuncFunctionStub) {
    indexNamedStub(stub)
    indexFunction(stub)
}

fun IndexSink.indexIncludePathDef(stub: FuncIncludePathStub) {
    indexNamedStub(stub)
    indexIncludePath(stub)
}

private fun IndexSink.indexNamedStub(stub: FuncNamedStub) = stub.name?.let {
    occurrence(FuncNamedElementIndex.KEY, it)
}

private fun IndexSink.indexFunction(stub: FuncFunctionStub) = stub.name?.let {
    occurrence(FuncFunctionIndex.KEY, it)
}

private fun IndexSink.indexIncludePath(stub: FuncIncludePathStub) = stub.name?.let {
    occurrence(FuncIncludeIndex.KEY, it)
}
