package org.ton.intellij.tolk.codeInsight

import org.ton.intellij.tolk.ide.configurable.tolkSettings
import org.ton.intellij.tolk.inspection.TolkExpectTypeBuiltinInspection

class TolkAnnotationCodeInsightTest : TolkCodeInsightBaseTest() {
    override fun setUp() {
        super.setUp()
        val file = myFixture.copyDirectoryToProject("tolk-stdlib", "tolk-stdlib")
        project.tolkSettings.explicitPathToStdlib = file.url
        myFixture.enableInspections(TolkExpectTypeBuiltinInspection::class.java)
    }

    fun `test annotation`() = doTest()
}
