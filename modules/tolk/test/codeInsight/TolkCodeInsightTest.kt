package org.ton.intellij.tolk.codeInsight

class TolkCodeInsightTest : TolkCodeInsightBaseTest() {
    override fun getTestDataPath(): String = "${super.testDataPath}/tests"

    fun `test a-tests`() = doTest()
}
