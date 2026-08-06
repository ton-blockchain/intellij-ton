package org.ton.intellij.acton.toml

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class ActonTomlCompletionContributorTest : BasePlatformTestCase() {
    fun testBuildOutDirProvidesPathCompletion() {
        myFixture.addFileToProject("generated/output/result.json", "{}")
        myFixture.addFileToProject("generated/wrappers/output.ts", "export {}")

        assertCompletionContains(
            """
                [build]
                out-dir = "generated/<caret>"
            """.trimIndent(),
            "output",
            "wrappers",
        )
    }

    fun testBuildOutputAbiProvidesPathCompletion() {
        myFixture.addFileToProject("generated/abi/counter.json", "{}")

        assertCompletionContains(
            """
                [build]
                output-abi = "generated/<caret>"
            """.trimIndent(),
            "abi",
        )
    }

    fun testBuildOutputSourcesProvidesPathCompletion() {
        myFixture.addFileToProject("generated/sources/counter.json", "{}")

        assertCompletionContains(
            """
                [build]
                output-sources = "generated/<caret>"
            """.trimIndent(),
            "sources",
        )
    }

    fun testContractTypesProvidesPathCompletion() {
        myFixture.addFileToProject("interfaces/counter.types.tolk", "contract Counter {}")

        assertCompletionContains(
            """
                [contracts.counter]
                src = "contracts/counter.boc"
                types = "interfaces/<caret>"
            """.trimIndent(),
            "counter.types.tolk",
        )
    }

    fun testGasProfileProvidesPathCompletion() {
        myFixture.addFileToProject("profiles/gas.json", "{}")

        assertCompletionContains(
            """
                [test]
                gas-profile = "profiles/<caret>"
            """.trimIndent(),
            "gas.json",
        )
    }

    fun testLocalnetDbPathProvidesPathCompletion() {
        myFixture.addFileToProject("state/localnet.sqlite", "")

        assertCompletionContains(
            """
                [localnet]
                db-path = "state/<caret>"
            """.trimIndent(),
            "localnet.sqlite",
        )
    }

    fun testDependencyPathProvidesPathCompletion() {
        myFixture.addFileToProject("generated/contracts/counter.code.fc", "// generated")
        myFixture.addFileToProject("generated/wrappers/counter.wrapper.tolk", "fun main() {}")

        assertCompletionContains(
            """
                [contracts.counter]
                src = "contracts/counter.tolk"

                [contracts.app]
                src = "contracts/app.tolk"
                depends = [{ name = "counter", path = "generated/<caret>" }]
            """.trimIndent(),
            "contracts",
            "wrappers",
        )
    }

    fun testLitenodeAccountsProvidesWalletNameCompletion() {
        myFixture.addFileToProject(
            "wallets.toml",
            """
                [wallets.alice]
                mnemonic = "local"

                [wallets.bob]
                mnemonic = "local"
            """.trimIndent(),
        )

        assertCompletionContains(
            """
                [litenode]
                accounts = ["<caret>"]
            """.trimIndent(),
            "alice",
            "bob",
        )
    }

    fun testDependsProvidesContractIdCompletion() {
        assertCompletionContains(
            """
                [contracts.counter]
                src = "contracts/counter.tolk"

                [contracts.wallet]
                src = "contracts/wallet.tolk"

                [contracts.app]
                src = "contracts/app.tolk"
                depends = ["<caret>"]
            """.trimIndent(),
            "counter",
            "wallet",
        )
    }

    fun testLintRuleOverrideProvidesContractIdKeyCompletion() {
        assertCompletionContains(
            """
                [contracts.counter]
                src = "contracts/counter.tolk"

                [contracts.core]
                src = "contracts/core.tolk"

                [lint.rules.some_rule]
                co<caret>
            """.trimIndent(),
            "counter",
            "core",
        )
    }

    fun testForkNetCustomProvidesCustomNetworkCompletion() {
        assertCompletionContains(
            """
                [networks.devnet]

                [networks.staging]

                [test]
                fork-net = { custom = "<caret>" }
            """.trimIndent(),
            "devnet",
            "staging",
        )
    }

    fun testMutationDisableRulesProvidesRuleIdCompletion() {
        assertCompletionContains(
            """
                [test.mutation]
                disable-rules = ["flip_<caret>"]
            """.trimIndent(),
            "flip_plus",
            "flip_minus",
            "flip_gt_ge",
        )
    }

    fun testPackageLicenseProvidesKnownOptions() {
        assertCompletionContains(
            """
                [package]
                license = "A<caret>"
            """.trimIndent(),
            "Apache-2.0",
        )
    }

    fun testFmtIgnoreProvidesGlobCompletion() {
        myFixture.addFileToProject("contracts/counter.tolk", "fun main() {}")
        myFixture.addFileToProject("scripts/deploy.tolk", "fun main() {}")

        assertCompletionContains(
            """
                [fmt]
                ignore = ["<caret>"]
            """.trimIndent(),
            "contracts/",
            "contracts/**",
            "scripts/",
            "scripts/**",
        )
    }

    fun testTestIncludeProvidesNestedGlobCompletion() {
        myFixture.addFileToProject("tests/unit/counter.test.tolk", "fun test() {}")
        myFixture.addFileToProject("tests/integration/deploy.test.tolk", "fun test() {}")

        assertCompletionContains(
            """
                [test]
                include = ["tests/<caret>"]
            """.trimIndent(),
            "tests/integration/",
            "tests/integration/**",
            "tests/unit/",
            "tests/unit/**",
        )
    }

    fun testLintExcludeProvidesFileCompletion() {
        myFixture.addFileToProject("generated/report.json", "{}")
        myFixture.addFileToProject("generated/runtime.log", "log")
        myFixture.addFileToProject("generated/trace.log", "log")

        assertCompletionContains(
            """
                [lint]
                exclude = ["generated/r<caret>"]
            """.trimIndent(),
            "generated/report.json",
            "generated/runtime.log",
        )
    }

    private fun assertCompletionContains(code: String, vararg expected: String) {
        myFixture.configureByText("Acton.toml", code)
        myFixture.completeBasic()
        val lookups = myFixture.lookupElementStrings.orEmpty()
        for (item in expected) {
            assertTrue("Expected completion `$item`, got $lookups", item in lookups)
        }
    }
}
