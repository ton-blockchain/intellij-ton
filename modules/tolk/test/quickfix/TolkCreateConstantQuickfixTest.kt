package org.ton.intellij.tolk.quickfix

import com.intellij.codeInspection.InspectionProfileEntry
import org.intellij.lang.annotations.Language
import org.ton.intellij.tolk.ide.configurable.tolkSettings
import org.ton.intellij.tolk.inspection.TolkUnresolvedReferenceInspection

class TolkCreateConstantQuickfixTest : TolkQuickfixTestBase() {
    override fun setUp() {
        super.setUp()
        val file = myFixture.copyDirectoryToProject("tolk-stdlib", "tolk-stdlib")
        project.tolkSettings.explicitPathToStdlib = file.url
        myFixture.enableInspections(TolkUnresolvedReferenceInspection::class.java)
    }

    fun `test create constant basic`() = doQuickfixTest(
        """
            fun test() {
                var x = MY_CONSTANT/*caret*/;
            }
        """.trimIndent(),
        """
            const MY_CONSTANT = 0
            
            fun test() {
                var x = MY_CONSTANT;
            }
        """.trimIndent(),
        "Create constant",
    )

    fun `test create constant in expression`() = doQuickfixTest(
        """
            fun calculate() {
                return MAX_VALUE/*caret*/ + 10;
            }
        """.trimIndent(),
        """
            const MAX_VALUE = 0
            
            fun calculate() {
                return MAX_VALUE + 10;
            }
        """.trimIndent(),
        "Create constant",
    )

    fun `test create constant in assignment`() = doQuickfixTest(
        """
            fun init() {
                var limit = DEFAULT_LIMIT/*caret*/;
            }
        """.trimIndent(),
        """
            const DEFAULT_LIMIT = 0
            
            fun init() {
                var limit = DEFAULT_LIMIT;
            }
        """.trimIndent(),
        "Create constant",
    )

    fun `test create constant in condition`() = doQuickfixTest(
        """
            fun check(value: int) {
                if (value > THRESHOLD/*caret*/) {
                    return true;
                }
                return false;
            }
        """.trimIndent(),
        """
            const THRESHOLD = 0
            
            fun check(value: int) {
                if (value > THRESHOLD) {
                    return true;
                }
                return false;
            }
        """.trimIndent(),
        "Create constant",
    )

    fun `test create constant in function call`() = doQuickfixTest(
        """
            fun process() {
                doWork(BUFFER_SIZE/*caret*/);
            }
        """.trimIndent(),
        """
            const BUFFER_SIZE = 0
            
            fun process() {
                doWork(BUFFER_SIZE);
            }
        """.trimIndent(),
        "Create constant",
    )

    fun `test create constant in arithmetic`() = doQuickfixTest(
        """
            fun compute(value: int) {
                return value * MULTIPLIER/*caret*/;
            }
        """.trimIndent(),
        """
            const MULTIPLIER = 0
            
            fun compute(value: int) {
                return value * MULTIPLIER;
            }
        """.trimIndent(),
        "Create constant",
    )

    fun `test create constant from other constant value`() = doQuickfixTest(
        """
            const OTHER = DEFAULT_TIMEOUT/*caret*/;
        """.trimIndent(),
        """
            const DEFAULT_TIMEOUT = 0
            
            const OTHER = DEFAULT_TIMEOUT;
        """.trimIndent(),
        "Create constant",
    )

    fun `test create constant from struct field value`() = doQuickfixTest(
        """
            struct Foo {
                foo: int = DEFAULT_TIMEOUT/*caret*/;
            }
        """.trimIndent(),
        """
            const DEFAULT_TIMEOUT = 0
            
            struct Foo {
                foo: int = DEFAULT_TIMEOUT;
            }
        """.trimIndent(),
        "Create constant",
    )
}
