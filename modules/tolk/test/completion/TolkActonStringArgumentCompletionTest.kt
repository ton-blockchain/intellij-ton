package org.ton.intellij.tolk.completion

class TolkActonStringArgumentCompletionTest : TolkCompletionTestBase() {
    fun `test scripts wallet provides wallet name completion`() {
        myFixture.addFileToProject("Acton.toml", "")
        myFixture.addFileToProject(
            "wallets.toml",
            """
                [wallets.alice]
                mnemonic = "local"

                [wallets.bob]
                mnemonic = "local"
            """.trimIndent(),
        )

        checkContainsCompletion(
            listOf("alice", "bob"),
            """
                fun main() {
                    scripts.wallet("/*caret*/")
                }
            """.trimIndent(),
        )
    }

    fun `test net wallet does not provide wallet name completion`() {
        myFixture.addFileToProject("Acton.toml", "")
        myFixture.addFileToProject(
            "wallets.toml",
            """
                [wallets.alice]
                mnemonic = "local"
            """.trimIndent(),
        )

        checkNotContainsCompletion(
            "alice",
            """
                fun main() {
                    net.wallet("/*caret*/")
                }
            """.trimIndent(),
        )
    }
}
