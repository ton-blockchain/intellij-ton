package org.ton.intellij.tolk.stub

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubBase
import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef

abstract class StubWithText<T : PsiElement?>(
    parent: StubElement<*>?,
    elementType: IStubElementType<*, *>?,
    private val text: StringRef?,
) : StubBase<T>(parent, elementType), TextHolder {

    override fun getText() = text?.string
}
