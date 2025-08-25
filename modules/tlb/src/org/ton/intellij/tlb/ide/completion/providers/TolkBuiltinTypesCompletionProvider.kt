package org.ton.intellij.tlb.ide.completion.providers

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import org.ton.intellij.tlb.TlbIcons
import org.ton.intellij.tlb.ide.completion.TlbCompletionProvider
import org.ton.intellij.tlb.psi.TlbTypeExpression

object TolkBuiltinTypesCompletionProvider : TlbCompletionProvider() {
    override val elementPattern: ElementPattern<out PsiElement> =
        PlatformPatterns.psiElement()
            .withParent(TlbTypeExpression::class.java)

    val BUILTIN_TYPES: Map<String, String> = mapOf(
        "#" to "Nat, 32-bit unsigned integer",
        "##" to "Nat: unsigned integer with `x` bits",
        "#<" to "Nat: unsigned integer less than `x` stored with the minimum number `⌈log2 x⌉` of bits (up to 31) to represent the number `x`",
        "#<=" to "Nat: unsigned integer less than or equal `x` stored with the minimum number `⌈log2(x+1)⌉` of bits (up to 32) to represent the number `x`",
        "Any" to "Remaining bits and references",
        "Cell" to "Remaining bits and references",
        "Int" to "257 bits",
        "UInt" to "256 bits",
        "Bits" to "1023 bits",
        "bits" to "X bits",
        "uint" to "",
        "uint8" to "",
        "uint16" to "",
        "uint32" to "",
        "uint64" to "",
        "uint128" to "",
        "uint256" to "",
        "int" to "",
        "int8" to "",
        "int16" to "",
        "int32" to "",
        "int64" to "",
        "int128" to "",
        "int256" to "",
        "int257" to "",
        "Type" to "Built-in TL-B type representing the type of types",
    )

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        BUILTIN_TYPES.forEach { (type, description) ->
            result.addElement(
                LookupElementBuilder.create(type)
                    .withTailText(" $description", true)
                    .withIcon(TlbIcons.TYPE)
            )
        }
    }
}
