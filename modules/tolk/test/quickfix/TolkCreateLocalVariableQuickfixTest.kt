package org.ton.intellij.tolk.quickfix

import org.ton.intellij.tolk.ide.configurable.tolkSettings
import org.ton.intellij.tolk.inspection.TolkUnresolvedReferenceInspection

class TolkCreateLocalVariableQuickfixTest : TolkQuickfixTestBase() {
    override fun setUp() {
        super.setUp()
        val file = myFixture.copyDirectoryToProject("tolk-stdlib", "tolk-stdlib")
        project.tolkSettings.stdlibPath = file.url
        myFixture.enableInspections(TolkUnresolvedReferenceInspection::class.java)
    }

    fun `test create local variable basic`() = doQuickfixTest(
        """
            fun test() {
                var x = localVar/*caret*/;
            }
        """.trimIndent(),
        """
            fun test() {
                val localVar = 0;
                var x = localVar;
            }
        """.trimIndent(),
        "Create local variable",
    )

    fun `test create local variable in expression`() = doQuickfixTest(
        """
            fun calculate() {
                return value/*caret*/ + 10;
            }
        """.trimIndent(),
        """
            fun calculate() {
                val value = 0;
                return value + 10;
            }
        """.trimIndent(),
        "Create local variable",
    )

    fun `test create local variable in assignment`() = doQuickfixTest(
        """
            fun process() {
                result/*caret*/ = 42;
            }
        """.trimIndent(),
        """
            fun process() {
                val result = 0;
                result = 42;
            }
        """.trimIndent(),
        "Create local variable",
    )

    fun `test create local variable in condition`() = doQuickfixTest(
        """
            fun check() {
                if (flag/*caret*/) {
                    return true;
                }
                return false;
            }
        """.trimIndent(),
        """
            fun check() {
                val flag = 0;
                if (flag) {
                    return true;
                }
                return false;
            }
        """.trimIndent(),
        "Create local variable",
    )

    fun `test create local variable in loop`() = doQuickfixTest(
        """
            fun iterate() {
                while (i/*caret*/ < 10) {
                    i = i + 1;
                }
            }
        """.trimIndent(),
        """
            fun iterate() {
                val i = 0;
                while (i < 10) {
                    i = i + 1;
                }
            }
        """.trimIndent(),
        "Create local variable",
    )

    fun `test create local variable in function call`() = doQuickfixTest(
        """
            fun process() {
                doWork(temp/*caret*/);
            }
        """.trimIndent(),
        """
            fun process() {
                val temp = 0;
                doWork(temp);
            }
        """.trimIndent(),
        "Create local variable",
    )

    fun `test create local variable in nested scope`() = doQuickfixTest(
        """
            fun test() {
                if (true) {
                    var x = nestedVar/*caret*/;
                }
            }
        """.trimIndent(),
        """
            fun test() {
                if (true) {
                    val nestedVar = 0;
                    var x = nestedVar;
                }
            }
        """.trimIndent(),
        "Create local variable",
    )
}
