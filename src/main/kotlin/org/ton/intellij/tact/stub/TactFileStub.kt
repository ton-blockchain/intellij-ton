package org.ton.intellij.tact.stub

import com.intellij.psi.stubs.PsiFileStubImpl
import org.ton.intellij.func.FuncFileElementType
import org.ton.intellij.tact.psi.TactFile

class TactFileStub(
    file: TactFile?,
) : PsiFileStubImpl<TactFile>(file) {
    override fun getType() = FuncFileElementType
}
