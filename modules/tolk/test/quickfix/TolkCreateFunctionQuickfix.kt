package org.ton.intellij.tolk.quickfix

import org.ton.intellij.tolk.ide.configurable.tolkSettings
import org.ton.intellij.tolk.inspection.TolkUnresolvedReferenceInspection

class TolkCreateFunctionQuickfix : TolkQuickfixTestBase() {
    override fun setUp() {
        super.setUp()
        val file = myFixture.copyDirectoryToProject("tolk-stdlib", "tolk-stdlib")
        project.tolkSettings.stdlibPath = file.url
        myFixture.enableInspections(TolkUnresolvedReferenceInspection::class.java)
    }

    fun `test call without arguments`() = doQuickfixTest(
        """
            fun foo() {
                unknownFunction/*caret*/();
            }
        """.trimIndent(),
        """
            fun unknownFunction() {
                
            }

            fun foo() {
                unknownFunction();
            }
        """.trimIndent(),
    )

    fun `test call with single literal argument`() = doQuickfixTest(
        """
            fun foo() {
                unknownFunction/*caret*/(10);
            }
        """.trimIndent(),
        """
            fun unknownFunction(param0: int) {
                
            }

            fun foo() {
                unknownFunction(10);
            }
        """.trimIndent(),
    )

    fun `test call with single string argument`() = doQuickfixTest(
        """
            fun foo() {
                logMessage/*caret*/("hello");
            }
        """.trimIndent(),
        """
            fun logMessage(param0: slice) {
                
            }

            fun foo() {
                logMessage("hello");
            }
        """.trimIndent(),
    )

    fun `test call with boolean argument`() = doQuickfixTest(
        """
            fun foo() {
                setFlag/*caret*/(true);
            }
        """.trimIndent(),
        """
            fun setFlag(param0: bool) {
                
            }

            fun foo() {
                setFlag(true);
            }
        """.trimIndent(),
    )

    fun `test call with mixed arguments`() = doQuickfixTest(
        """
            fun foo() {
                var count: int = 5;
                mixedCall/*caret*/(count, "literal", 42);
            }
        """.trimIndent(),
        """
            fun mixedCall(count: int, param1: slice, param2: int) {
                
            }

            fun foo() {
                var count: int = 5;
                mixedCall(count, "literal", 42);
            }
        """.trimIndent(),
    )

    fun `test call from nested scope`() = doQuickfixTest(
        """
            fun main() {
                if (true) {
                    var x: int = 10;
                    nestedFunction/*caret*/(x);
                }
            }
        """.trimIndent(),
        """
            fun nestedFunction(x: int) {
                
            }

            fun main() {
                if (true) {
                    var x: int = 10;
                    nestedFunction(x);
                }
            }
        """.trimIndent(),
    )

    fun `test call with function parameter`() = doQuickfixTest(
        """
            fun caller(param: int) {
                helper/*caret*/(param);
            }
        """.trimIndent(),
        """
            fun helper(param: int) {
                
            }

            fun caller(param: int) {
                helper(param);
            }
        """.trimIndent(),
    )

    fun `test call with complex expression`() = doQuickfixTest(
        """
            fun test() {
                var a: int = 5;
                var b: int = 10;
                complexCall/*caret*/(a + b, a * 2);
            }
        """.trimIndent(),
        """
            fun complexCall(param0: int, param1: int) {
                
            }

            fun test() {
                var a: int = 5;
                var b: int = 10;
                complexCall(a + b, a * 2);
            }
        """.trimIndent(),
    )

    fun `test call with null argument`() = doQuickfixTest(
        """
            fun test() {
                handleNull/*caret*/(null);
            }
        """.trimIndent(),
        """
            fun handleNull(param0: null) {
                
            }

            fun test() {
                handleNull(null);
            }
        """.trimIndent(),
    )

    fun `test call inside loop`() = doQuickfixTest(
        """
            fun test() {
                var i: int = 0;
                while (i < 10) {
                    loopFunction/*caret*/(i);
                    i = i + 1;
                }
            }
        """.trimIndent(),
        """
            fun loopFunction(i: int) {
                
            }

            fun test() {
                var i: int = 0;
                while (i < 10) {
                    loopFunction(i);
                    i = i + 1;
                }
            }
        """.trimIndent(),
    )

    fun `test call with string literal`() = doQuickfixTest(
        """
            fun test() {
                logMessage/*caret*/("Debug info");
            }
        """.trimIndent(),
        """
            fun logMessage(param0: slice) {
                
            }

            fun test() {
                logMessage("Debug info");
            }
        """.trimIndent(),
    )

    fun `test call with boolean literal`() = doQuickfixTest(
        """
            fun test() {
                setFlag/*caret*/(false);
            }
        """.trimIndent(),
        """
            fun setFlag(param0: bool) {
                
            }

            fun test() {
                setFlag(false);
            }
        """.trimIndent(),
    )

    fun `test call inside try-catch`() = doQuickfixTest(
        """
            fun test() {
                try {
                    riskyOperation/*caret*/();
                } catch (e) {
                    // handle error
                }
            }
        """.trimIndent(),
        """
            fun riskyOperation() {
                
            }

            fun test() {
                try {
                    riskyOperation();
                } catch (e) {
                    // handle error
                }
            }
        """.trimIndent(),
    )

    fun `test call with multiple types`() = doQuickfixTest(
        """
            fun test() {
                var num: int = 42;
                var text: slice = "hello";
                var flag: bool = true;
                multiType/*caret*/(num, text, flag);
            }
        """.trimIndent(),
        """
            fun multiType(num: int, text: slice, flag: bool) {
                
            }

            fun test() {
                var num: int = 42;
                var text: slice = "hello";
                var flag: bool = true;
                multiType(num, text, flag);
            }
        """.trimIndent(),
    )

    fun `test call from function with return type`() = doQuickfixTest(
        """
            fun getData(): int {
                processData/*caret*/();
                return 0;
            }
        """.trimIndent(),
        """
            fun processData() {
                
            }

            fun getData(): int {
                processData();
                return 0;
            }
        """.trimIndent(),
    )
}
