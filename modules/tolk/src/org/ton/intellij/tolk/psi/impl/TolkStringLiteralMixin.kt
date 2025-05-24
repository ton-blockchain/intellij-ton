package org.ton.intellij.tolk.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.LiteralTextEscaper
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.PsiReference
import org.ton.intellij.tolk.psi.TolkAsmBody
import org.ton.intellij.tolk.psi.TolkIncludeDefinition
import org.ton.intellij.tolk.psi.TolkStringLiteral
import org.ton.intellij.tolk.psi.reference.TolkLiteralFileReferenceSet

abstract class TolkStringLiteralMixin(node: ASTNode) : ASTWrapperPsiElement(node), TolkStringLiteral {
    override fun isValidHost(): Boolean {
        return parent is TolkAsmBody
    }

    override fun updateText(text: String): PsiLanguageInjectionHost {
        return this
    }

    override fun getReferences(): Array<out PsiReference?> {
        if (parent is TolkIncludeDefinition) {
            val rawString = rawString ?: return PsiReference.EMPTY_ARRAY
            val contents = rawString.text
            val fileSet = TolkLiteralFileReferenceSet(contents, this, rawString.startOffsetInParent)
            return fileSet.allReferences
        }
        return PsiReference.EMPTY_ARRAY
    }

    override fun createLiteralTextEscaper() = object : LiteralTextEscaper<TolkStringLiteralMixin>(this) {
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
