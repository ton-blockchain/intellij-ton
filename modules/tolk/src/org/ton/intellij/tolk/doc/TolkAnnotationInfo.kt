package org.ton.intellij.tolk.doc

object TolkAnnotationInfo {
    private val ANNOTATIONS_INFO = mapOf(
        "inline" to AnnotationInfo(
            "Function with this annotation will be automatically inlined during compilation",
        ),
        "inline_ref" to AnnotationInfo(
            "Function with this annotation will be automatically inlined by reference during compilation",
        ),
        "noinline" to AnnotationInfo(
            "Function with this annotation will not be inlined even if compiler can inline it",
        ),
        "pure" to AnnotationInfo(
            "Function with this annotation has no side effects and can be optimized away by the compiler",
        ),
        "deprecated" to AnnotationInfo(
            "Symbol with this annotation is deprecated and should not be used in new code. " +
                "First string argument is a reason for deprecation as a string literal.",
        ),
        "overflow1023_policy" to AnnotationInfo(
            "Defines the policy for handling potential builder overflow. " +
                "Right now, only \"suppress\" value is supported. " +
                "See <a href=\"https://docs.ton.org/v3/documentation/smart-contracts/tolk/tolk-vs-func/pack-to-from-cells#what-if-data-exceeds-1023-bits\">documentation</a> for more details",
        ),
        "on_bounced_policy" to AnnotationInfo(
            "Defines the policy for handling bounced messages. " +
                "Right now, only \"manual\" value is supported.",
        ),
        "method_id" to AnnotationInfo(
            "Specifies the method ID (as a number literal) for the function in smart contract interface. " +
                "See <a href=\"https://docs.ton.org/v3/guidelines/smart-contracts/get-methods\">documentation</a> for more details",
        ),
        "abi" to AnnotationInfo(
            "Describes ABI metadata for declaration.",
        ),
        "test" to AnnotationInfo(
            """
                Describes additional metadata for test function.
                <ul>
                <li><code>@test.skip</code> skips the test.</li>
                <li><code>@test.todo</code> marks the test as TODO.</li>
                <li><code>@test.todo("...")</code> marks the test as TODO with a description.</li>
                <li><code>@test.fail_with(42)</code> declares the expected exit code.</li>
                <li><code>@test.gas_limit(1000)</code> overrides the per-test gas limit.</li>
                <li><code>@test.fuzz</code> enables fuzzing with default settings.</li>
                <li><code>@test.fuzz(64)</code> enables fuzzing with 64 runs.</li>
                <li><code>@test.fuzz({ ... })</code> enables fuzzing with explicit config.</li>
                </ul>
            """.trimIndent(),
        ),
        "test.skip" to AnnotationInfo(
            "Marks the test as skipped.",
        ),
        "test.todo" to AnnotationInfo(
            "Marks the test as TODO. Use <code>@test.todo(\"...\")</code> to attach a description.",
        ),
        "test.fail_with" to AnnotationInfo(
            "Declares the expected exit code for the test.",
        ),
        "test.gas_limit" to AnnotationInfo(
            "Overrides the per-test gas limit.",
        ),
        "test.fuzz" to AnnotationInfo(
            "Enables fuzzing for parameterized tests. Supports <code>@test.fuzz</code>, <code>@test.fuzz(64)</code>, and <code>@test.fuzz({ ... })</code>.",
        ),
    )

    fun getAnnotationInfo(name: String): AnnotationInfo? =
        ANNOTATIONS_INFO[name] ?: ANNOTATIONS_INFO[name.substringBefore('.')]

    data class AnnotationInfo(val description: String)
}
