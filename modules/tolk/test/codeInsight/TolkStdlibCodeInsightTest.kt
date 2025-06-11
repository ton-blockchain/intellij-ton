package org.ton.intellij.tolk.codeInsight

import org.jetbrains.annotations.NonNls
import org.ton.intellij.tolk.ide.configurable.tolkSettings

@Suppress("SpellCheckingInspection")
class TolkStdlibCodeInsightTest : TolkCodeInsightBaseTest() {
    override fun getTestDataPath(): @NonNls String = "${super.testDataPath}/tolk-stdlib"

    override fun setUp() {
        super.setUp()
        val file = myFixture.copyDirectoryToProject(".", ".")
        project.tolkSettings.explicitPathToStdlib = file.url
    }

    fun `test common`() = doTest()
    fun `test gas-payments`() = doTest()
    fun `test lisp-lists`() = doTest()
    fun `test tvm-dicts`() = doTest()
    fun `test tvm-lowlevel`() = doTest()
}
