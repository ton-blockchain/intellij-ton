package org.ton.intellij.acton.ide

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class ActonWrapperFreshnessTest : BasePlatformTestCase() {
    fun testFindsTolkWrapperWhenConfiguredPathUniquelyIdentifiesContract() {
        myFixture.addFileToProject(
            "Acton.toml",
            """
                [contracts.voter]
                src = "contracts/voter-contract.tolk"

                [wrappers.tolk]
                output-dir = "generated"
            """.trimIndent(),
        )
        myFixture.addFileToProject("contracts/voter-contract.tolk", "contract Voter {}")
        val wrapper = myFixture.addFileToProject("generated/VoterContract.gen.tolk", "")

        val target = findActonWrapperTarget(project, wrapper.virtualFile)

        assertNotNull(target)
        assertEquals("voter", target!!.contractId)
        assertEquals(ActonWrapperLanguage.TOLK, target.language)
        assertEquals("voter-contract.tolk", target.sourceFile.name)
    }

    fun testFindsTypeScriptWrapperAtDefaultPath() {
        myFixture.addFileToProject(
            "Acton.toml",
            """
                [contracts.counter]
                src = "contracts/counter.tolk"
            """.trimIndent(),
        )
        myFixture.addFileToProject("contracts/counter.tolk", "contract Counter {}")
        val wrapper = myFixture.addFileToProject("wrappers-ts/Counter.gen.ts", "")

        val target = findActonWrapperTarget(project, wrapper.virtualFile)

        assertNotNull(target)
        assertEquals("counter", target!!.contractId)
        assertEquals(ActonWrapperLanguage.TYPESCRIPT, target.language)
    }

    fun testFindsTypeScriptWrapperAtConfiguredPath() {
        myFixture.addFileToProject(
            "Acton.toml",
            """
                [contracts.counter]
                src = "contracts/counter.tolk"

                [wrappers.typescript]
                output-dir = "generated-ts"
            """.trimIndent(),
        )
        myFixture.addFileToProject("contracts/counter.tolk", "contract Counter {}")
        val wrapper = myFixture.addFileToProject("generated-ts/Counter.gen.ts", "")

        val target = findActonWrapperTarget(project, wrapper.virtualFile)

        assertNotNull(target)
        assertEquals("counter", target!!.contractId)
        assertEquals(ActonWrapperLanguage.TYPESCRIPT, target.language)
    }

    fun testFindsTolkWrapperFromWrappersMapping() {
        myFixture.addFileToProject(
            "Acton.toml",
            """
                [contracts.counter]
                src = "contracts/counter.tolk"

                [import-mappings]
                "@wrappers" = "generated"
            """.trimIndent(),
        )
        myFixture.addFileToProject("contracts/counter.tolk", "contract Counter {}")
        val wrapper = myFixture.addFileToProject("generated/Counter.gen.tolk", "")

        val target = findActonWrapperTarget(project, wrapper.virtualFile)

        assertNotNull(target)
        assertEquals("counter", target!!.contractId)
        assertEquals(ActonWrapperLanguage.TOLK, target.language)
    }

    fun testDoesNotGuessWrapperFromAnUnconfiguredPath() {
        myFixture.addFileToProject(
            "Acton.toml",
            """
                [contracts.counter]
                src = "contracts/counter.tolk"
            """.trimIndent(),
        )
        myFixture.addFileToProject("contracts/counter.tolk", "contract Counter {}")
        val wrapper = myFixture.addFileToProject("custom/Counter.gen.tolk", "")

        assertNull(findActonWrapperTarget(project, wrapper.virtualFile))
    }

    fun testDoesNotChooseBetweenContractsWithTheSameGeneratedWrapperName() {
        myFixture.addFileToProject(
            "Acton.toml",
            """
                [contracts.first]
                src = "contracts/counter.tolk"

                [contracts.second]
                src = "other/counter.tolk"
            """.trimIndent(),
        )
        myFixture.addFileToProject("contracts/counter.tolk", "contract Counter {}")
        myFixture.addFileToProject("other/counter.tolk", "contract Counter {}")
        val wrapper = myFixture.addFileToProject("wrappers/Counter.gen.tolk", "")

        assertNull(findActonWrapperTarget(project, wrapper.virtualFile))
    }

    fun testDoesNotAssociateUnsupportedWrapperFile() {
        myFixture.addFileToProject(
            "Acton.toml",
            """
                [contracts.counter]
                src = "contracts/counter.tolk"
            """.trimIndent(),
        )
        myFixture.addFileToProject("contracts/counter.tolk", "contract Counter {}")
        val wrapper = myFixture.addFileToProject("wrappers/Counter.gen.js", "")

        assertNull(findActonWrapperTarget(project, wrapper.virtualFile))
    }
}
