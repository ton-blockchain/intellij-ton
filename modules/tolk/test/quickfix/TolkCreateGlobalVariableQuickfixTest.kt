package org.ton.intellij.tolk.quickfix

import org.ton.intellij.tolk.ide.configurable.tolkSettings
import org.ton.intellij.tolk.inspection.TolkUnresolvedReferenceInspection

class TolkCreateGlobalVariableQuickfixTest : TolkQuickfixTestBase() {
    override fun setUp() {
        super.setUp()
        val file = myFixture.copyDirectoryToProject("tolk-stdlib", "tolk-stdlib")
        project.tolkSettings.explicitPathToStdlib = file.url
        myFixture.enableInspections(TolkUnresolvedReferenceInspection::class.java)
    }

    fun `test create global variable basic`() = doQuickfixTest(
        """
            fun test() {
                var x = globalCounter/*caret*/;
            }
        """.trimIndent(),
        """
            global globalCounter: int;
            
            fun test() {
                var x = globalCounter;
            }
        """.trimIndent(),
        "Create global variable",
    )

    fun `test create global variable in assignment`() = doQuickfixTest(
        """
            fun update() {
                totalCount/*caret*/ = 100;
            }
        """.trimIndent(),
        """
            global totalCount: int;
            
            fun update() {
                totalCount = 100;
            }
        """.trimIndent(),
        "Create global variable",
    )

    fun `test create global variable in expression`() = doQuickfixTest(
        """
            fun calculate() {
                return currentValue/*caret*/ + 10;
            }
        """.trimIndent(),
        """
            global currentValue: int;
            
            fun calculate() {
                return currentValue + 10;
            }
        """.trimIndent(),
        "Create global variable",
    )

    fun `test create global variable in condition`() = doQuickfixTest(
        """
            fun check() {
                if (isInitialized/*caret*/) {
                    return true;
                }
                return false;
            }
        """.trimIndent(),
        """
            global isInitialized: int;
            
            fun check() {
                if (isInitialized) {
                    return true;
                }
                return false;
            }
        """.trimIndent(),
        "Create global variable",
    )

    fun `test create global variable in function call`() = doQuickfixTest(
        """
            fun process() {
                doWork(sharedData/*caret*/);
            }
        """.trimIndent(),
        """
            global sharedData: int;
            
            fun process() {
                doWork(sharedData);
            }
        """.trimIndent(),
        "Create global variable",
    )

    fun `test create global variable in constant value`() = doQuickfixTest(
        """
            const FOO = currentValue
        """.trimIndent(),
        """
            global currentValue: int;
            
            const FOO = currentValue
        """.trimIndent(),
        "Create global variable",
    )

}
