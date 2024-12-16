package org.ton.intellij.func.converter

import com.intellij.psi.PsiElement
import com.intellij.psi.util.endOffset
import com.intellij.psi.util.startOffset
import org.ton.intellij.func.psi.FuncPrimitiveType

class FuncToTolkState(
    private var funcSource: String,
    private var options: ConvertFuncToTolkOptions,
    private var forkedFromPsi: PsiElement? = null,
    private var lastEndOffset: Int = 0,
    var selfVarNameInModifyingMethod: String? = null,
    private val localVarNames: MutableSet<String> = mutableSetOf()
) {
    private val tolkSource = StringBuilder()
    private val warnings = mutableListOf<FuncConversationWarning>()
    private val knownGetMethods = mutableSetOf<String>()
    private val importStdlib = mutableSetOf<String>()

    var needInsertTolkPreamble = false
    val isInsideModifyingMethod get() = selfVarNameInModifyingMethod != null

    fun createEmptyFork(offsetPsi: PsiElement): FuncToTolkState {
       return FuncToTolkState(
            funcSource,
            options,
            offsetPsi,
            offsetPsi.endOffset,
            selfVarNameInModifyingMethod,
            localVarNames
        )
    }

    fun merge(vararg forksAndDelimiters: FuncToTolkState?) {
        var minStartOffset = Int.MAX_VALUE
        var maxEndOffset = 0
        for (fork in forksAndDelimiters) {
            val forkedPsi = fork?.forkedFromPsi
            if (forkedPsi != null) {
                minStartOffset = minOf(minStartOffset, forkedPsi.startOffset)
                maxEndOffset = maxOf(maxEndOffset, forkedPsi.endOffset)
            }
        }
        if (minStartOffset > lastEndOffset) {
            tolkSource.append(funcSource.substring(lastEndOffset, minStartOffset))
        }
        for (fork in forksAndDelimiters) {
            if (fork != null) {
                tolkSource.append(fork.tolkSource)
                warnings.addAll(fork.warnings)
            }
        }
        lastEndOffset = maxEndOffset
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

    fun autoImportStdlib(stdlibFile: String) {
        importStdlib.add(stdlibFile)
    }

    fun onEnterFunction(localVarNames: Set<String>) {
        this.localVarNames.addAll(localVarNames)
    }

    fun addTextCustom(text: String) {
        this.tolkSource.append(text)
    }

    private val FIRST_DERICTIVE_REGEX = Regex("#include|#pragma")
    private val VAR_REDEF_REGEX = Regex("var (\\w+) redef =")

    fun resultingTolkSource(): String {
        val output = StringBuilder()

        val posFirstDirective = maxOf(0, FIRST_DERICTIVE_REGEX.find(funcSource)?.range?.first ?: 0)
        output.append(tolkSource.substring(0, posFirstDirective))

        if (needInsertTolkPreamble) {
            output.append("tolk 0.6\n\n")
        }
        for (file in importStdlib) {
            output.append("import \"$file\"\n")
        }

        output.append(tolkSource.substring(posFirstDirective))
        output.replace(VAR_REDEF_REGEX, "$1 =")
        output.append("\n")

        return output.toString()
    }

    fun addTextModified(psiElement: PsiElement, text: String) {
        if (psiElement.startOffset > lastEndOffset) {
            tolkSource.append(funcSource.substring(lastEndOffset, psiElement.startOffset))
        }
        tolkSource.append(text)
        lastEndOffset = psiElement.endOffset
    }

    fun addTextUnchanged(psiElement: PsiElement) {
        if (psiElement.startOffset > lastEndOffset) {
            tolkSource.append(funcSource.substring(lastEndOffset, psiElement.startOffset))
        }
        tolkSource.append(psiElement.text)
        lastEndOffset = psiElement.endOffset
    }
}
