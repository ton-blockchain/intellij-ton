package org.ton.intellij.func.stub

import com.intellij.psi.stubs.PsiFileStub
import com.intellij.psi.stubs.PsiFileStubImpl
import com.intellij.psi.tree.IStubFileElementType
import org.ton.intellij.func.FuncFileElementType
import org.ton.intellij.func.psi.FuncFile

class FuncFileStub(
    file: FuncFile?
) : PsiFileStubImpl<FuncFile>(file) {
    override fun getType() = FuncFileElementType

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is FuncFileStub
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }
}
