package org.ton.intellij.tact.psi

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.psi.FileViewProvider
import org.ton.intellij.tact.TactFileType
import org.ton.intellij.tact.TactLanguage
import org.ton.intellij.tact.stub.TactFileStub

class TactFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, TactLanguage) {
    override fun getFileType() = TactFileType

    override fun getStub() = super.getStub() as? TactFileStub
}
