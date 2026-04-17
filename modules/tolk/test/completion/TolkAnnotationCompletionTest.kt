package org.ton.intellij.tolk.completion

class TolkAnnotationCompletionTest : TolkCompletionTestBase() {
    fun `test root annotation completion variants after at`() = checkContainsCompletion(
        listOf("deprecated", "test", "method_id"),
        """
            @/*caret*/
            get fun `test sample`() {}
        """.trimIndent(),
    )

    fun `test root test annotation completion without args`() = checkCompletion(
        "test",
        """
            @te/*caret*/
            get fun `test sample`() {}
        """.trimIndent(),
        """
            @test/*caret*/
            get fun `test sample`() {}
        """.trimIndent(),
        '\t',
    )

    fun `test test annotation dotted completion variants`() = checkContainsCompletion(
        listOf("fail_with", "fuzz"),
        """
            @test.f/*caret*/
            get fun `test sample`() {}
        """.trimIndent(),
    )

    fun `test no nested completion for unsupported dotted annotation`() = checkNoCompletion(
        """
            @abi./*caret*/
            get fun `test sample`() {}
        """.trimIndent(),
    )

    fun `test no annotation completion inside string literal`() = checkNoCompletion(
        """
            fun sample() {
                val value = "@te/*caret*/"
            }
        """.trimIndent(),
    )

    fun `test no annotation completion inside comment`() = checkNoCompletion(
        """
            fun sample() {
                // @te/*caret*/
            }
        """.trimIndent(),
    )

    fun `test test skip annotation completion`() = checkCompletion(
        "skip",
        """
            @test.sk/*caret*/
            get fun `test sample`() {}
        """.trimIndent(),
        """
            @test.skip/*caret*/
            get fun `test sample`() {}
        """.trimIndent(),
        '\t',
    )

    fun `test test todo annotation completion without args`() = checkCompletion(
        "todo",
        """
            @test.to/*caret*/
            get fun `test sample`() {}
        """.trimIndent(),
        """
            @test.todo/*caret*/
            get fun `test sample`() {}
        """.trimIndent(),
        '\t',
    )

    fun `test test todo annotation completion with string arg`() = checkCompletion(
        "todo",
        "(...)",
        """
            @test.to/*caret*/
            get fun `test sample`() {}
        """.trimIndent(),
        """
            @test.todo("")/*caret*/
            get fun `test sample`() {}
        """.trimIndent(),
        '\t',
    )

    fun `test test fail_with annotation completion`() = checkCompletion(
        "fail_with",
        """
            @test.fa/*caret*/
            get fun `test sample`() {}
        """.trimIndent(),
        """
            @test.fail_with(/*caret*/)
            get fun `test sample`() {}
        """.trimIndent(),
        '\t',
    )

    fun `test test gas_limit annotation completion`() = checkCompletion(
        "gas_limit",
        """
            @test.ga/*caret*/
            get fun `test sample`() {}
        """.trimIndent(),
        """
            @test.gas_limit(/*caret*/)
            get fun `test sample`() {}
        """.trimIndent(),
        '\t',
    )

    fun `test test fuzz annotation completion without args`() = checkCompletion(
        "fuzz",
        """
            @test.fu/*caret*/
            get fun `test sample`() {}
        """.trimIndent(),
        """
            @test.fuzz/*caret*/
            get fun `test sample`() {}
        """.trimIndent(),
        '\t',
    )

    fun `test test fuzz annotation completion with args`() = checkCompletion(
        "fuzz",
        "(...)",
        """
            @test.fu/*caret*/
            get fun `test sample`() {}
        """.trimIndent(),
        """
            @test.fuzz(/*caret*/)
            get fun `test sample`() {}
        """.trimIndent(),
        '\t',
    )
}
