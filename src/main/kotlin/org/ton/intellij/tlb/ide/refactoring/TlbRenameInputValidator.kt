package org.ton.intellij.tlb.ide.refactoring

import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.StandardPatterns
import com.intellij.psi.PsiElement
import com.intellij.refactoring.rename.RenameInputValidator
import com.intellij.util.ProcessingContext
import org.ton.intellij.tlb.psi.TlbConstructor
import org.ton.intellij.tlb.psi.TlbElementTypes
import org.ton.intellij.tlb.psi.TlbExplicitField
import org.ton.intellij.tlb.psi.TlbTypeDef

class TlbRenameInputValidator : RenameInputValidator {
    override fun getPattern(): ElementPattern<out PsiElement> {
        return StandardPatterns.or(
            PlatformPatterns.psiElement(TlbElementTypes.CONSTRUCTOR),
            PlatformPatterns.psiElement(TlbElementTypes.TYPE_DEF),
            PlatformPatterns.psiElement(TlbElementTypes.EXPLICIT_FIELD),
        )
    }

    override fun isInputValid(newName: String, element: PsiElement, context: ProcessingContext): Boolean {
        if (newName.isEmpty()) {
            return false
        }
        val identifierKind = IdentifierKind.forIdentifier(newName)
        if (element is TlbConstructor) {
            if (newName != "_" && !identifierKind.isLowerCase()) {
                return false
            }
        }
        if (element is TlbTypeDef) {
            if (!identifierKind.isUpperCase()) {
                return false
            }
        }
        if (element is TlbExplicitField) {
            if (newName != "_" && !identifierKind.isLowerCase()) {
                return false
            }
        }
        return true
    }

    enum class IdentifierKind {
        Undefined,
        LowerCase,
        UpperCase,
        BangLowerCase;

        fun isUpperCase() = this == UpperCase
        fun isLowerCase() = this == LowerCase || this == BangLowerCase

        companion object {
            // Ported from
            // https://github.com/ton-blockchain/ton/blob/5c392e0f2d946877bb79a09ed35068f7b0bd333a/crypto/tl/tlbc.cpp#L175
            fun forIdentifier(identifier: String): IdentifierKind {
                var result = Undefined
                var unicodeChar = 0
                var isBang = 0
                for (char in identifier) {
                    if (char == '.') {
                        result = Undefined
                        isBang = 0
                        unicodeChar = 0
                    } else if (result == Undefined) {
                        if (isBang == 0) {
                            isBang = if (char == '!') 1 else -1
                        }
                        if (char.isLetter()) {
                            result = if (char.isLowerCase()) LowerCase else UpperCase
                        }
                        if (unicodeChar != 0 && (char.code and 0xc0 == 0x80)) {
                            unicodeChar = (unicodeChar shl 6) or (char.code and 0x3f)
                            if (unicodeChar in 0x410 until 0x450) {
                                result = if (unicodeChar < 0x430) UpperCase else LowerCase
                            }
                        }
                        unicodeChar = if (char.code and 0xe0 == 0xc0) char.code and 0x1f else 0
                    }
                }
                if (isBang == 1 && result == LowerCase) {
                    result = BangLowerCase
                }
                return result
            }
        }
    }
}
