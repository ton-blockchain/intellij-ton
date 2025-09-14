package org.ton.intellij.tolk.quickfix.quickfix

import org.ton.intellij.tolk.ide.configurable.tolkSettings
import org.ton.intellij.tolk.inspection.TolkUnresolvedReferenceInspection
import org.ton.intellij.tolk.inspection.TolkUnusedVariableInspection
import org.ton.intellij.tolk.quickfix.TolkQuickfixTestBase

class TolkRenameToUnderscoreQuickfixTest : TolkQuickfixTestBase() {
    override fun setUp() {
        super.setUp()
        val file = myFixture.copyDirectoryToProject("tolk-stdlib", "tolk-stdlib")
        project.tolkSettings.explicitPathToStdlib = file.url
        myFixture.enableInspections(TolkUnusedVariableInspection::class.java)
    }

    fun `test rename unused variable`() = doQuickfixTest(
        """
            fun test() {
                var a/*caret*/ = 100;
            }
        """.trimIndent(),
        """
            fun test() {
                var _ = 100;
            }
        """.trimIndent(),
        "Rename element",
    )

    fun `test rename unused variable in tensor variable`() = doQuickfixTest(
        """
            fun test() {
                var (a: int, b/*caret*/: int) = (1, 2);
                throw a;
            }
        """.trimIndent(),
        """
            fun test() {
                var (a: int, _) = (1, 2);
                throw a;
            }
        """.trimIndent(),
        "Rename element",
    )

    fun `test rename unused variable in tuple variable`() = doQuickfixTest(
        """
            fun test() {
                var [a: int, b/*caret*/: int] = [1, 2];
                throw a;
            }
        """.trimIndent(),
        """
            fun test() {
                var [a: int, _] = [1, 2];
                throw a;
            }
        """.trimIndent(),
        "Rename element",
    )
}
