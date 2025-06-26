package org.ton.intellij.tolk.codeInsight

import org.jetbrains.annotations.NonNls
import org.ton.intellij.tolk.TolkTestBase
import org.ton.intellij.tolk.inspection.TolkExpectTypeBuiltinInspection

abstract class TolkCodeInsightBaseTest : TolkTestBase() {
    override fun getTestDataPath(): @NonNls String = "testResources/org.ton.intellij.tolk"

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(TolkExpectTypeBuiltinInspection::class.java)
    }

    protected fun doTest() {
        val name = getTestName(false)
        myFixture.testHighlighting("$name.tolk")
    }
}
