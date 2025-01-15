package org.ton.intellij.tolk.stub

import com.intellij.psi.stubs.PsiFileStubImpl
import org.ton.intellij.tolk.TolkFileElementType
import org.ton.intellij.tolk.psi.TolkFile

class TolkFileStub(
    file: TolkFile?,
) : PsiFileStubImpl<TolkFile>(file) {
    override fun getType() = TolkFileElementType
}
