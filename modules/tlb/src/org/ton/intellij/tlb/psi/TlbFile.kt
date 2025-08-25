package org.ton.intellij.tlb.psi

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.FileViewProvider
import org.ton.intellij.tlb.TlbFileType
import org.ton.intellij.tlb.TlbLanguage

class TlbFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, TlbLanguage), TlbElement {
    override fun getFileType(): FileType = TlbFileType
    override fun toString(): String = "TLB"

    fun findResultTypes(name: String): Sequence<TlbResultType> = sequence {
        val constructors = findChildrenByClass(TlbConstructor::class.java)
        for (constructor in constructors) {
            val resultType = constructor.resultType ?: continue
            if (resultType.name == name) {
                yield(resultType)
            }
        }
    }

    fun resultTypes(): Sequence<TlbResultType> = sequence {
        val constructors = findChildrenByClass(TlbConstructor::class.java)
        for (constructor in constructors) {
            val resultType = constructor.resultType ?: continue
            yield(resultType)
        }
    }

    fun constructors(): List<TlbConstructor> {
        return findChildrenByClass(TlbConstructor::class.java).toList()
    }
}
