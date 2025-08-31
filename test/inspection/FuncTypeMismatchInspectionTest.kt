package inspection

import org.ton.intellij.func.inspection.FuncTypeMismatchInspection

class FuncTypeMismatchInspectionTest : FuncInspectionTestBase() {

    fun `test simple assignment type mismatch`() {
        doInspectionTest(
            """
            () main() impure {
                int a = <error descr="Cannot assign 'slice' to variable of type 'int'">""</error>;
            }
            """.trimIndent(),
            FuncTypeMismatchInspection()
        )
    }

    fun `test function parameter type mismatch`() {
        doInspectionTest(
            """
            () foo(int a) impure { }
            
            () main() impure {
                foo(<error descr="Cannot pass 'slice' to parameter of type 'int'">"data"</error>);
            }
            """.trimIndent(),
            FuncTypeMismatchInspection()
        )
    }

    fun `test second function parameter type mismatch`() {
        doInspectionTest(
            """
            () foo(int a, slice b) impure { }
            
            () main() impure {
                foo(1, <error descr="Cannot pass 'int' to parameter of type 'slice'">1</error>);
            }
            """.trimIndent(),
            FuncTypeMismatchInspection()
        )
    }

    fun `test method call without parameters`() {
        doInspectionTest(
            """
            builder begin_cell() asm "NEWC";
            cell end_cell(builder b) asm "ENDC";
            
            () main() impure {
                begin_cell().end_cell();
            }
            """.trimIndent(),
            FuncTypeMismatchInspection()
        )
    }

    fun `test method call with parameter no type mismatch`() {
        doInspectionTest(
            """
            builder begin_cell() asm "NEWC";
            cell end_cell(builder b) asm "ENDC";
            builder store_ref(builder b, cell c) asm(c b) "STREF";
            
            () main() impure {
                begin_cell().store_ref(begin_cell().end_cell());
            }
            """.trimIndent(),
            FuncTypeMismatchInspection()
        )
    }

    fun `test method call with parameter type mismatch`() {
        doInspectionTest(
            """
            builder begin_cell() asm "NEWC";
            builder store_ref(builder b, cell c) asm(c b) "STREF";
            
            () main() impure {
                begin_cell().store_ref(<error descr="Cannot pass 'int' to parameter of type 'cell'">10</error>);
            }
            """.trimIndent(),
            FuncTypeMismatchInspection()
        )
    }

    fun `test function return type mismatch`() {
        doInspectionTest(
            """
            (slice, slice) get_slice() {
                return <error descr="Cannot return '(slice, int)' from function with return type '(slice, slice)'">("", 10)</error>;
            }
            """.trimIndent(),
            FuncTypeMismatchInspection()
        )
    }

    fun `test tensor destructuring size mismatch`() {
        doInspectionTest(
            """
            (slice, slice, int) get_slice() {
                return ("hello", "world", 10);
            }
            
            () main() impure {
                (slice a, slice b) = <error descr="Cannot destructure tensor of 3 elements into 2 variables">get_slice()</error>;
            }
            """.trimIndent(),
            FuncTypeMismatchInspection()
        )
    }

    fun `test tensor destructuring size mismatch 2`() {
        doInspectionTest(
            """
            (slice, slice) get_slice() {
                return ("hello", "world");
            }
            
            () main() impure {
                (slice a, slice b, int c) = <error descr="Cannot destructure tensor of 2 elements into 3 variables">get_slice()</error>;
            }
            """.trimIndent(),
            FuncTypeMismatchInspection()
        )
    }

    fun `test tensor destructuring type mismatch`() {
        doInspectionTest(
            """
            (slice, slice) get_slice() {
                return ("hello", "world");
            }
            
            () main() impure {
                (<error descr="Cannot assign 'slice' to variable of type 'int'">int a</error>, <error descr="Cannot assign 'slice' to variable of type 'int'">int b</error>) = get_slice();
            }
            """.trimIndent(),
            FuncTypeMismatchInspection()
        )
    }

    fun `test tensor destructuring type mismatch 2`() {
        doInspectionTest(
            """
            (slice, int) get_slice() {
                return ("hello", 10);
            }
            
            () main() impure {
               (<error descr="Cannot assign 'slice' to variable of type 'int'">int a</error>, <error descr="Cannot assign 'int' to variable of type 'slice'">slice b</error>) = get_slice();
            }
            """.trimIndent(),
            FuncTypeMismatchInspection()
        )
    }

    fun `test tuple destructuring size mismatch`() {
        doInspectionTest(
            """
            () main() impure {
                [var a, int b] = <error descr="Cannot destructure tuple of 3 elements into 2 variables">[1, "slice", 10]</error>;
            }
            """.trimIndent(),
            FuncTypeMismatchInspection()
        )
    }

    fun `test tuple destructuring size mismatch 2`() {
        doInspectionTest(
            """
            () main() impure {
                [var a, int b, int c] = <error descr="Cannot destructure tuple of 2 elements into 3 variables">[1, "slice"]</error>;
            }
            """.trimIndent(),
            FuncTypeMismatchInspection()
        )
    }

    fun `test tuple destructuring type mismatch`() {
        doInspectionTest(
            """
            () main() impure {
                [int a, <error descr="Cannot assign 'slice' to variable of type 'int'">int b</error>] = [1, "slice"];
            }
            """.trimIndent(),
            FuncTypeMismatchInspection()
        )
    }

    fun `test var tensor destructuring size mismatch`() {
        doInspectionTest(
            """
            (slice, slice) get_slice() {
                return ("hello", "world");
            }
            
            () main() impure {
                var (a, b, c) = <error descr="Cannot destructure tensor of 2 elements into 3 variables">get_slice()</error>;
            }
            """.trimIndent(),
            FuncTypeMismatchInspection()
        )
    }

    fun `test var tensor destructuring size mismatch 2`() {
        doInspectionTest(
            """
            (slice, slice, int) get_slice() {
                return ("hello", "world", 1);
            }
            
            () main() impure {
                var (a, b) = <error descr="Cannot destructure tensor of 3 elements into 2 variables">get_slice()</error>;
            }
            """.trimIndent(),
            FuncTypeMismatchInspection()
        )
    }

    fun `test ternary expression no type mismatch`() {
        doInspectionTest(
            """
            () main(int cond) impure {
                var b = cond ? 1 : 10;
            }
            """.trimIndent(),
            FuncTypeMismatchInspection()
        )
    }

    fun `test ternary expression type mismatch`() {
        doInspectionTest(
            """
            () main(int cond) impure {
                var b = <error descr="Incompatible types in ternary expression: 'int' and 'slice'">cond ? 1 : ""</error>;
            }
            """.trimIndent(),
            FuncTypeMismatchInspection()
        )
    }

    fun `test no false positives for correct code`() {
        checkNoProblems(
            """
            (slice, slice) get_slice() {
                return ("hello", "world");
            }
            
            () foo(int a) impure { }
            
            () main(int cond) impure {
                int a = 42;
                foo(123);
                (slice x, slice y) = get_slice();
                [int p, int q] = [1, 2];
                var (u, v) = get_slice();
                var correct = cond ? 1 : 2;
            }
            """.trimIndent(),
            FuncTypeMismatchInspection()
        )
    }
}