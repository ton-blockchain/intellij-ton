package org.ton.intellij.acton.toml

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.toml.lang.psi.TomlKeySegment

class ActonTomlLineMarkerProviderTest : BasePlatformTestCase() {
    private val provider = ActonTomlLineMarkerProvider()

    fun testLintSectionProvidesRunLinterAction() {
        val info = lineMarkerInfoFor(
            """
                [lin<caret>t]
                output-format = "sarif"
            """.trimIndent()
        )

        val actionTexts = info.actions.mapNotNull { it.templateText }
        assertContainsElements(actionTexts, "Run Linter")
    }

    fun testFmtSectionProvidesFormattingActions() {
        val info = lineMarkerInfoFor(
            """
                [fm<caret>t]
                width = 100
            """.trimIndent()
        )

        val actionTexts = info.actions.mapNotNull { it.templateText }
        assertContainsElements(actionTexts, "Check Formatting", "Format Project")
    }

    private fun lineMarkerInfoFor(code: String): com.intellij.execution.lineMarker.RunLineMarkerContributor.Info {
        val file = myFixture.configureByText("Acton.toml", code)
        val element = file.findElementAt(myFixture.caretOffset)?.parent as? TomlKeySegment
        assertNotNull(element)

        return provider.getInfo(element!!)
            ?: throw AssertionError("Expected line marker info for `${element.text}`")
    }
}
