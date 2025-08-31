package inspection

import FuncTestBase
import com.intellij.codeInspection.InspectionProfileEntry
import com.intellij.codeInspection.LocalInspectionTool
import org.intellij.lang.annotations.Language

abstract class FuncInspectionTestBase : FuncTestBase() {
    override fun getTestDataPath(): String = "testResources/"

    protected fun doInspectionTest(
        @Language("FunC") code: String,
        vararg inspections: InspectionProfileEntry,
        checkWarnings: Boolean = false,
        checkInfos: Boolean = false,
        checkWeakWarnings: Boolean = false
    ) {
        myFixture.enableInspections(*inspections)
        myFixture.configureByText("test.fc", code)
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
        @Language("FunC") code: String,
        inspection: LocalInspectionTool,
        checkWarnings: Boolean = false,
        checkInfos: Boolean = false,
        checkWeakWarnings: Boolean = false
    ) {
        myFixture.enableInspections(inspection)
        myFixture.configureByText("test.fc", code)
        myFixture.testHighlighting(checkWarnings, checkInfos, checkWeakWarnings)
    }

    protected fun checkNoProblems(
        @Language("FunC") code: String,
        vararg inspections: InspectionProfileEntry
    ) {
        myFixture.enableInspections(*inspections)
        myFixture.configureByText("test.fc", code)

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