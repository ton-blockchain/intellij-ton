package org.ton.intellij.tolk.codeInsight

import org.jetbrains.annotations.NonNls
import org.ton.intellij.tolk.TolkTestBase

abstract class TolkCodeInsightBaseTest : TolkTestBase() {
    override fun getTestDataPath(): @NonNls String = "testResources/org.ton.intellij.tolk"

    protected fun doTest() {
        val name = getTestName(false)
        myFixture.testHighlighting("$name.tolk")
    }
}
