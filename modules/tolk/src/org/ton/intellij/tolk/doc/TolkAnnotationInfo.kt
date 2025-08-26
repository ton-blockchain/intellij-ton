package org.ton.intellij.tolk.doc

object TolkAnnotationInfo {
    private val ANNOTATIONS_INFO = mapOf(
        "inline" to AnnotationInfo(
            "Function with this annotation will be automatically inlined during compilation"
        ),
        "inline_ref" to AnnotationInfo(
            "Function with this annotation will be automatically inlined by reference during compilation"
        ),
        "noinline" to AnnotationInfo(
            "Function with this annotation will not be inlined even if compiler can inline it"
        ),
        "pure" to AnnotationInfo(
            "Function with this annotation has no side effects and can be optimized away by the compiler"
        ),
        "deprecated" to AnnotationInfo(
            "Symbol with this annotation is deprecated and should not be used in new code. " +
                    "First string argument is a reason for deprecation as a string literal."
        ),
        "overflow1023_policy" to AnnotationInfo(
            "Defines the policy for handling potential builder overflow. " +
                    "Right now, only \"suppress\" value is supported. " +
                    "See <a href=\"https://docs.ton.org/v3/documentation/smart-contracts/tolk/tolk-vs-func/pack-to-from-cells#what-if-data-exceeds-1023-bits\">documentation</a> for more details"
        ),
        "on_bounced_policy" to AnnotationInfo(
            "Defines the policy for handling bounced messages. " +
                    "Right now, only \"manual\" value is supported."
        ),
        "method_id" to AnnotationInfo(
            "Specifies the method ID (as a number literal) for the function in smart contract interface. " +
                    "See <a href=\"https://docs.ton.org/v3/guidelines/smart-contracts/get-methods\">documentation</a> for more details"
        )
    )

    fun getAnnotationInfo(name: String): AnnotationInfo? = ANNOTATIONS_INFO[name]

    data class AnnotationInfo(
        val description: String,
    )
}
