package org.ton.intellij.func.converter

import com.intellij.psi.PsiElement
import com.intellij.psi.util.endOffset
import com.intellij.psi.util.startOffset

class FuncToTolkState(
    private var funcSource: String,
    private var options: ConvertFuncToTolkOptions
) {
    private val knownGetMethods = mutableSetOf<String>()
    private val tolkSource = StringBuilder()
    private var lastEndOffset = 0

    var selfVarNameInModifyingMethod: String? = null
    val isInsideModifyingMethod get() = selfVarNameInModifyingMethod != null

    fun createEmptyFork(offsetPsi: PsiElement): FuncToTolkState {
        val fork = FuncToTolkState(funcSource, options)

        return fork
    }

    fun registerKnownGetMethod(functionName: String) {
        knownGetMethods.add(functionName)
    }

    fun justSkipPsi(psiElement: PsiElement, withTrailingNl: Boolean = true) {
        if (psiElement.startOffset > lastEndOffset) {
            tolkSource.append(funcSource.substring(lastEndOffset, psiElement.startOffset))
        }
        lastEndOffset = psiElement.endOffset
        if (withTrailingNl && funcSource[lastEndOffset] == '\n') {
            lastEndOffset++
        }
    }


}
