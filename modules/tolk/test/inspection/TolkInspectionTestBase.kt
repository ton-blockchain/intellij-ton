package org.ton.intellij.tolk.inspection

import com.intellij.codeInspection.InspectionProfileEntry
import com.intellij.codeInspection.LocalInspectionTool
import org.intellij.lang.annotations.Language
import org.ton.intellij.tolk.TolkTestBase
import org.ton.intellij.tolk.ide.configurable.tolkSettings

abstract class TolkInspectionTestBase : TolkTestBase() {
    override fun getTestDataPath(): String = "testResources/org.ton.intellij.tolk"

    override fun setUp() {
        super.setUp()
        val file = myFixture.copyDirectoryToProject("tolk-stdlib", "tolk-stdlib")
        project.tolkSettings.explicitPathToStdlib = file.url
    }

    protected fun doInspectionTest(
        @Language("Tolk") code: String,
        vararg inspections: InspectionProfileEntry,
        checkWarnings: Boolean = false,
        checkInfos: Boolean = false,
        checkWeakWarnings: Boolean = false
    ) {
        myFixture.enableInspections(*inspections)
        myFixture.configureByText("test.tolk", code)
        myFixture.testHighlighting(checkWarnings, checkInfos, checkWeakWarnings)
    }

    protected fun doInspectionTestFromFile(
        fileName: String,
        vararg inspections: InspectionProfileEntry,
        checkWarnings: Boolean = false,
        checkInfos: Boolean = false,
        checkWeakWarnings: Boolean = false
    ) {
        myFixture.enableInspections(*inspections)
        myFixture.testHighlighting(checkWarnings, checkInfos, checkWeakWarnings, fileName)
    }

    protected fun doSingleInspectionTest(
        @Language("Tolk") code: String,
        inspection: LocalInspectionTool,
        checkWarnings: Boolean = false,
        checkInfos: Boolean = false,
        checkWeakWarnings: Boolean = false
    ) {
        myFixture.enableInspections(inspection)
        myFixture.configureByText("test.tolk", code)
        myFixture.testHighlighting(checkWarnings, checkInfos, checkWeakWarnings)
    }

    protected fun checkNoProblems(
        @Language("Tolk") code: String,
        vararg inspections: InspectionProfileEntry
    ) {
        myFixture.enableInspections(*inspections)
        myFixture.configureByText("test.tolk", code)
        
        val highlights = myFixture.doHighlighting()
        val problemHighlights = highlights.filter { 
            it.severity.name in setOf("ERROR", "WARNING", "WEAK_WARNING", "INFO") 
        }
        
        assertTrue(
            "Expected no problems, but found: ${problemHighlights.joinToString { "${it.severity}: ${it.description}" }}",
            problemHighlights.isEmpty()
        )
    }
}
