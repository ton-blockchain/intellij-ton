package org.ton.intellij.tolk.refactor

class TolkIntroduceVariableTest : TolkRefactorTestBase() {

    fun `test introduce variable from literal`() = doRefactorTest(
        """
            fun test() {
                var x = 42/*caret*/;
            }
        """.trimIndent(),
        """
            fun test() {
                val name = 42;
                var x = name;
            }
        """.trimIndent(),
        TolkIntroduceVariableHandler()
    )

    fun `test introduce variable from expression`() = doRefactorTestWithSelection(
        """
            fun test() {
                var result = /*selection*/5 + 3/*selection-end*/;
            }
        """.trimIndent(),
        """
            fun test() {
                val name = 5 + 3;
                var result = name;
            }
        """.trimIndent(),
        TolkIntroduceVariableHandler()
    )

    fun `test introduce variable from function call`() = doRefactorTestWithSelection(
        """
            fun getValue(): int {
                return 42;
            }
            
            fun test() {
                var x = /*selection*/getValue()/*selection-end*/;
            }
        """.trimIndent(),
        """
            fun getValue(): int {
                return 42;
            }
            
            fun test() {
                val name = getValue();
                var x = name;
            }
        """.trimIndent(),
        TolkIntroduceVariableHandler()
    )

    fun `test introduce variable with selection`() = doRefactorTestWithSelection(
        """
            fun test() {
                var result = /*selection*/10 * 5/*selection-end*/ + 3;
            }
        """.trimIndent(),
        """
            fun test() {
                val name = 10 * 5;
                var result = name + 3;
            }
        """.trimIndent(),
        TolkIntroduceVariableHandler()
    )

    fun `test introduce variable from complex expression`() = doRefactorTestWithSelection(
        """
            fun test() {
                var a = 5;
                var b = 10;
                var result = /*selection*/(a + b) * 2/*selection-end*/;
            }
        """.trimIndent(),
        """
            fun test() {
                var a = 5;
                var b = 10;
                val name = (a + b) * 2;
                var result = name;
            }
        """.trimIndent(),
        TolkIntroduceVariableHandler()
    )

    fun `test introduce variable from string literal`() = doRefactorTestWithSelection(
        """
            fun test() {
                var message = /*selection*/"Hello World"/*selection-end*/;
            }
        """.trimIndent(),
        """
            fun test() {
                val name = "Hello World";
                var message = name;
            }
        """.trimIndent(),
        TolkIntroduceVariableHandler()
    )

    fun `test introduce variable from boolean literal`() = doRefactorTestWithSelection(
        """
            fun test() {
                var flag = /*selection*/true/*selection-end*/;
            }
        """.trimIndent(),
        """
            fun test() {
                val name = true;
                var flag = name;
            }
        """.trimIndent(),
        TolkIntroduceVariableHandler()
    )

    fun `test introduce variable in condition`() = doRefactorTestWithSelection(
        """
            fun test() {
                if (/*selection*/5 > 3/*selection-end*/) {
                    return true;
                }
                return false;
            }
        """.trimIndent(),
        """
            fun test() {
                val name = 5 > 3;
                if (name) {
                    return true;
                }
                return false;
            }
        """.trimIndent(),
        TolkIntroduceVariableHandler()
    )

    fun `test introduce variable in return statement`() = doRefactorTestWithSelection(
        """
            fun test(): int {
                return /*selection*/10 + 20/*selection-end*/;
            }
        """.trimIndent(),
        """
            fun test(): int {
                val name = 10 + 20;
                return name;
            }
        """.trimIndent(),
        TolkIntroduceVariableHandler()
    )

    fun `test introduce variable with parentheses`() = doRefactorTestWithSelection(
        """
            fun test() {
                var result = /*selection*/(5 + 3)/*selection-end*/;
            }
        """.trimIndent(),
        """
            fun test() {
                val name = 5 + 3;
                var result = (name);
            }
        """.trimIndent(),
        TolkIntroduceVariableHandler()
    )

    fun `test introduce variable in arithmetic expression`() = doRefactorTestWithSelection(
        """
            fun test() {
                var a = 10;
                var result = /*selection*/a * 2/*selection-end*/ + 5;
            }
        """.trimIndent(),
        """
            fun test() {
                var a = 10;
                val name = a * 2;
                var result = name + 5;
            }
        """.trimIndent(),
        TolkIntroduceVariableHandler()
    )

    fun `test introduce variable from nested expression`() = doRefactorTestWithSelection(
        """
            fun test() {
                var x = 5;
                var y = 10;
                var result = x + (/*selection*/y * 2/*selection-end*/);
            }
        """.trimIndent(),
        """
            fun test() {
                var x = 5;
                var y = 10;
                val name = y * 2;
                var result = x + (name);
            }
        """.trimIndent(),
        TolkIntroduceVariableHandler()
    )

    fun `test introduce variable in loop condition`() = doRefactorTestWithSelection(
        """
            fun test() {
                var i = 0;
                while (/*selection*/i < 10/*selection-end*/) {
                    i = i + 1;
                }
            }
        """.trimIndent(),
        """
            fun test() {
                var i = 0;
                val name = i < 10;
                while (name) {
                    i = i + 1;
                }
            }
        """.trimIndent(),
        TolkIntroduceVariableHandler()
    )

    fun `test introduce variable from variable reference`() = doRefactorTestWithSelection(
        """
            fun test() {
                var original = 42;
                var copy = /*selection*/original/*selection-end*/;
            }
        """.trimIndent(),
        """
            fun test() {
                var original = 42;
                val name = original;
                var copy = name;
            }
        """.trimIndent(),
        TolkIntroduceVariableHandler()
    )

    fun `test introduce variable in nested blocks`() = doRefactorTestWithSelection(
        """
            fun test() {
                if (true) {
                    var x = /*selection*/5 + 3/*selection-end*/;
                }
            }
        """.trimIndent(),
        """
            fun test() {
                if (true) {
                    val name = 5 + 3;
                    var x = name;
                }
            }
        """.trimIndent(),
        TolkIntroduceVariableHandler()
    )

    fun `test introduce variable from comparison`() = doRefactorTestWithSelection(
        """
            fun test() {
                var a = 5;
                var b = 10;
                var result = /*selection*/a == b/*selection-end*/;
            }
        """.trimIndent(),
        """
            fun test() {
                var a = 5;
                var b = 10;
                val name = a == b;
                var result = name;
            }
        """.trimIndent(),
        TolkIntroduceVariableHandler()
    )

    fun `test introduce variable with multiple occurrences same block`() = doRefactorTestWithSelection(
        """
            fun test() {
                var result1 = /*selection*/5 + 3/*selection-end*/;
                var result2 = 5 + 3;
                var result3 = 5 + 3;
            }
        """.trimIndent(),
        """
            fun test() {
                val name = 5 + 3;
                var result1 = name;
                var result2 = name;
                var result3 = name;
            }
        """.trimIndent(),
        TolkIntroduceVariableHandler()
    )

    fun `test introduce variable with multiple occurrences different blocks`() = doRefactorTestWithSelection(
        """
            fun test() {
                if (true) {
                    var x = /*selection*/10 * 2/*selection-end*/;
                }
                if (false) {
                    var y = 10 * 2;
                }
                var z = 10 * 2;
            }
        """.trimIndent(),
        """
            fun test() {
                val name = 10 * 2;
                if (true) {
                    var x = name;
                }
                if (false) {
                    var y = name;
                }
                var z = name;
            }
        """.trimIndent(),
        TolkIntroduceVariableHandler()
    )

    fun `test introduce variable with nested occurrences`() = doRefactorTestWithSelection(
        """
            fun test() {
                var a = /*selection*/getValue()/*selection-end*/;
                if (a > 0) {
                    var b = getValue();
                    while (b < 10) {
                        var c = getValue();
                        b = b + 1;
                    }
                }
            }
        """.trimIndent(),
        """
            fun test() {
                val name = getValue();
                var a = name;
                if (a > 0) {
                    var b = name;
                    while (b < 10) {
                        var c = name;
                        b = b + 1;
                    }
                }
            }
        """.trimIndent(),
        TolkIntroduceVariableHandler()
    )

    fun `test introduce variable with complex expression multiple times`() = doRefactorTestWithSelection(
        """
            fun test() {
                var a = 5;
                var b = 10;
                var result1 = /*selection*/(a + b) * 2/*selection-end*/;
                var result2 = (a + b) * 2 + 1;
                var result3 = ((a + b) * 2);
            }
        """.trimIndent(),
        """
            fun test() {
                var a = 5;
                var b = 10;
                val name = (a + b) * 2;
                var result1 = name;
                var result2 = name + 1;
                var result3 = (name);
            }
        """.trimIndent(),
        TolkIntroduceVariableHandler()
    )

    fun `test introduce variable in loop with multiple uses`() = doRefactorTestWithSelection(
        """
            fun test() {
                var i = 0;
                while (i < 5) {
                    var temp = /*selection*/i * 2/*selection-end*/;
                    var doubled = i * 2;
                    if (i * 2 > 6) {
                        break;
                    }
                    i = i + 1;
                }
            }
        """.trimIndent(),
        """
            fun test() {
                var i = 0;
                while (i < 5) {
                    val name = i * 2;
                    var temp = name;
                    var doubled = name;
                    if (name > 6) {
                        break;
                    }
                    i = i + 1;
                }
            }
        """.trimIndent(),
        TolkIntroduceVariableHandler()
    )

    fun `test introduce variable with function calls in different contexts`() = doRefactorTestWithSelection(
        """
            fun helper(): int {
                return 42;
            }
            
            fun test() {
                var x = /*selection*/helper()/*selection-end*/;
                if (helper() > 0) {
                    var y = helper() + 1;
                    return helper();
                }
                return 0;
            }
        """.trimIndent(),
        """
            fun helper(): int {
                return 42;
            }
            
            fun test() {
                val name = helper();
                var x = name;
                if (name > 0) {
                    var y = name + 1;
                    return name;
                }
                return 0;
            }
        """.trimIndent(),
        TolkIntroduceVariableHandler()
    )

    fun `test introduce variable with string concatenation`() = doRefactorTestWithSelection(
        """
            fun test() {
                var prefix = "Hello";
                var msg1 = /*selection*/prefix + " World"/*selection-end*/;
                var msg2 = prefix + " World";
                var msg3 = (prefix + " World");
            }
        """.trimIndent(),
        """
            fun test() {
                var prefix = "Hello";
                val name = prefix + " World";
                var msg1 = name;
                var msg2 = name;
                var msg3 = (name);
            }
        """.trimIndent(),
        TolkIntroduceVariableHandler()
    )

    fun `test introduce variable with arithmetic in conditions`() = doRefactorTestWithSelection(
        """
            fun test() {
                var a = 10;
                var b = 5;
                if (/*selection*/a - b/*selection-end*/ > 0) {
                    var result = a - b;
                } else if (a - b < 0) {
                    var negative = -(a - b);
                }
                return a - b;
            }
        """.trimIndent(),
        """
            fun test() {
                var a = 10;
                var b = 5;
                val name = a - b;
                if (name > 0) {
                    var result = name;
                } else if (name < 0) {
                    var negative = -(name);
                }
                return name;
            }
        """.trimIndent(),
        TolkIntroduceVariableHandler()
    )

    fun `test introduce variable in try catch blocks`() = doRefactorTestWithSelection(
        """
            fun test() {
                try {
                    var result = /*selection*/riskyOperation()/*selection-end*/;
                    if (riskyOperation() > 0) {
                        return riskyOperation();
                    }
                } catch (e) {
                    var error = riskyOperation();
                    return error;
                }
                return 0;
            }
        """.trimIndent(),
        """
            fun test() {
                val name = riskyOperation();
                try {
                    var result = name;
                    if (name > 0) {
                        return name;
                    }
                } catch (e) {
                    var error = name;
                    return error;
                }
                return 0;
            }
        """.trimIndent(),
        TolkIntroduceVariableHandler()
    )
}
