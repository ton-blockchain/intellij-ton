package org.ton.intellij.tolk.codeInsight

import org.ton.intellij.tolk.ide.configurable.tolkSettings

class TolkAnnotationCodeInsightTest : TolkCodeInsightBaseTest() {
    override fun setUp() {
        super.setUp()
        val file = myFixture.copyDirectoryToProject("tolk-stdlib", "tolk-stdlib")
        project.tolkSettings.explicitPathToStdlib = file.url
    }

    fun `test annotation`() = doTest()
}
