package org.ton.intellij.func.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.LiteralTextEscaper
import com.intellij.psi.PsiLanguageInjectionHost
import org.ton.intellij.func.psi.FuncAsmBody
import org.ton.intellij.func.psi.FuncStringLiteral

abstract class FuncStringLiteralMixin(node: ASTNode) : ASTWrapperPsiElement(node), FuncStringLiteral {
    override fun isValidHost(): Boolean {
        return parent is FuncAsmBody
    }

    override fun updateText(text: String): PsiLanguageInjectionHost {
        return this
    }

    override fun createLiteralTextEscaper() = object : LiteralTextEscaper<FuncStringLiteralMixin>(this) {
        override fun decode(rangeInsideHost: TextRange, outChars: StringBuilder): Boolean {
            outChars.append(rangeInsideHost.substring(myHost.text))
            return true
        }

        override fun getOffsetInHost(offsetInDecoded: Int, rangeInsideHost: TextRange): Int {
            return rangeInsideHost.startOffset + offsetInDecoded
        }

        override fun isOneLine(): Boolean {
            return true
        }
    }
}
