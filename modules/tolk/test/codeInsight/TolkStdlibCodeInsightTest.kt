package org.ton.intellij.tolk.codeInsight

import org.jetbrains.annotations.NonNls

@Suppress("SpellCheckingInspection")
class TolkStdlibCodeInsightTest : TolkCodeInsightBaseTest() {
    override fun getTestDataPath(): @NonNls String = "${super.testDataPath}/tolk-stdlib"

    fun `test common`() = doTest()
    fun `test gas-payments`() = doTest()
    fun `test lisp-lists`() = doTest()
    fun `test tvm-dicts`() = doTest()
    fun `test tvm-lowlevel`() = doTest()
}
