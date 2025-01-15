package org.ton.intellij.tlb.inspection

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.LocalQuickFixOnPsiElement
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.codeInspection.util.IntentionFamilyName
import com.intellij.codeInspection.util.IntentionName
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.ton.intellij.tlb.psi.TlbResultType
import org.ton.intellij.tlb.psi.TlbVisitor
import org.ton.intellij.tlb.psi.tlbPsiFactory
import java.util.*

class TlbWrongResultTypeNameCaseInspection : TlbInspectionBase() {
    override fun buildTlbVisitor(
        holder: ProblemsHolder,
        session: LocalInspectionToolSession
    ): TlbVisitor = object : TlbVisitor() {
        override fun visitResultType(o: TlbResultType) {
            val symbolCase = determineSymbolCase(o.name ?: return)
            if (symbolCase == SymbolCase.UPPERCASE) return
            val identifier = o.identifier
            holder.registerProblem(
                identifier,
                "Type name must begin with an uppercase letter",
                TlbChangeCaseFix(identifier, true)
            )
        }
    }

    class TlbChangeCaseFix(private val identifier: PsiElement, private val toUppercase: Boolean) :
        LocalQuickFixOnPsiElement(identifier) {
        override fun getText(): @IntentionName String {
            return if (toUppercase) "Change to Uppercase"
            else "Change to Lowercase"
        }

        override fun invoke(
            project: Project,
            file: PsiFile,
            startElement: PsiElement,
            endElement: PsiElement
        ) {
            val name = identifier.text ?: return
            val newName = if (toUppercase) name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            else name.replaceFirstChar { if (it.isUpperCase()) it.lowercase(Locale.getDefault()) else it.toString() }
            identifier.replace(project.tlbPsiFactory.createIdentifier(newName))
        }

        override fun getFamilyName(): @IntentionFamilyName String {
            return "Change case"
        }
    }


    enum class SymbolCase {
        UNDEFINED,
        LOWERCASE,
        UPPERCASE,
        BANG_LOWERCASE
    }

    fun determineSymbolCase(input: String): SymbolCase {
        var state = SymbolCase.UNDEFINED
        var partialUtf8 = 0
        var firstCharSign = 0

        for (char in input) {
            if (char == '.') {
                state = SymbolCase.UNDEFINED
                partialUtf8 = 0
                firstCharSign = 0
            } else if (state == SymbolCase.UNDEFINED) {
                if (firstCharSign == 0) {
                    firstCharSign = if (char == '!') 1 else -1
                }
                if (char.isLetter()) {
                    state = if (char.isLowerCase()) SymbolCase.LOWERCASE else SymbolCase.UPPERCASE
                } else {
                    if (partialUtf8 != 0 && (char.code and 0xc0) == 0x80) {
                        partialUtf8 = (partialUtf8 shl 6) or (char.code and 0x3f)
                        if (partialUtf8 in 0x410 until 0x450) {
                            state = if (partialUtf8 < 0x430) SymbolCase.UPPERCASE else SymbolCase.LOWERCASE
                        }
                    }
                    partialUtf8 = if ((char.code and 0xe0) == 0xc0) (char.code and 0x1f) else 0
                }
            }
        }

        if (firstCharSign == 1 && state == SymbolCase.LOWERCASE) {
            state = SymbolCase.BANG_LOWERCASE
        }

        return state
    }
}