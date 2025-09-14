package org.ton.intellij.tolk.inspection

class TolkStructInitializationInspectionTest : TolkInspectionTestBase() {
    fun `test several missing fields initialization`() {
        doInspectionTest(
            """
            struct Point {
                x: int;
                y: int;
            }

            fun foo() {
                <error descr="Fields 'x', 'y' missed in initialization">Point</error>{};
            }
            """.trimIndent(),
            TolkStructInitializationInspection()
        )
    }

    fun `test several missing fields initialization with alias`() {
        doInspectionTest(
            """
            struct Point {
                x: int;
                y: int;
            }

            type PointAlias = Point;

            fun foo() {
                <error descr="Fields 'x', 'y' missed in initialization">PointAlias</error> {};
            }
            """.trimIndent(),
            TolkStructInitializationInspection()
        )
    }

    fun `test several missing fields initialization with generic struct`() {
        doInspectionTest(
            """
            struct Point<T> {
                x: T;
                y: T;
            }

            fun foo() {
                <error descr="Fields 'x', 'y' missed in initialization">Point<int></error> {};
            }
            """.trimIndent(),
            TolkStructInitializationInspection()
        )
    }

    fun `test several missing fields initialization with generic struct alias`() {
        doInspectionTest(
            """
            struct Point<T> {
                x: T;
                y: T;
            }

            type IntPoint = Point<int>;

            fun foo() {
                <error descr="Fields 'x', 'y' missed in initialization">IntPoint</error> {};
            }
            """.trimIndent(),
            TolkStructInitializationInspection()
        )
    }

    fun `test several missing fields initialization with short syntax`() {
        doInspectionTest(
            """
            struct Point {
                x: int;
                y: int;
            }

            fun foo() {
                val p: Point = <error descr="Fields 'x', 'y' missed in initialization">{</error>};
            }
            """.trimIndent(),
            TolkStructInitializationInspection()
        )
    }

    fun `test missing field initialization`() {
        doInspectionTest(
            """
            struct Point {
                x: int;
                y: int;
            }

            fun foo() {
                <error descr="Field 'y' missed in initialization">Point</error>{ x: 1 };
            }
            """.trimIndent(),
            TolkStructInitializationInspection()
        )
    }

    fun `test all fields with default values`() {
        doInspectionTest(
            """
            struct Point {
                x: int = 0;
                y: int = 0;
            }

            fun foo() {
                Point{};
                Point{ x: 1 };
                Point{ x: 1, y: 2 };
            }
            """.trimIndent(),
            TolkStructInitializationInspection()
        )
    }

    fun `test complete initialization`() {
        doInspectionTest(
            """
            struct Point {
                x: int;
                y: int;
            }

            fun foo() {
                Point{x: 1, y: 2};
            }
            """.trimIndent(),
            TolkStructInitializationInspection()
        )
    }

    fun `test complete initialization with backticked identifiers`() {
        doInspectionTest(
            """
            struct Point {
                x: int;
                `y`: int;
            }

            fun foo() {
                Point{`x`: 1, y: 2};
            }
            """.trimIndent(),
            TolkStructInitializationInspection()
        )
    }

    fun `test complete initialization with short syntax`() {
        doInspectionTest(
            """
            struct Point {
                x: int;
                y: int;
            }

            fun foo() {
                val x = 1;
                val y = 2;
                Point{x, y};
            }
            """.trimIndent(),
            TolkStructInitializationInspection()
        )
    }

    fun `test complete initialization with short instance syntax`() {
        doInspectionTest(
            """
            struct Point {
                x: int;
                y: int;
            }

            fun foo() {
                val x = 1;
                val y = 2;
                val point: Point = {x, y};
            }
            """.trimIndent(),
            TolkStructInitializationInspection()
        )
    }

    fun `test complete initialization with single short syntax`() {
        doInspectionTest(
            """
            struct Point {
                x: int;
                y: int;
            }

            fun foo() {
                val x = 1;
                val y = 2;
                Point{x: 10, y};
            }
            """.trimIndent(),
            TolkStructInitializationInspection()
        )
    }

    fun `test optional field`() {
        doInspectionTest(
            """
            struct Point {
                x: int?;
            }

            fun foo() {
                <error descr="Field 'x' missed in initialization">Point</error> {};
            }
            """.trimIndent(),
            TolkStructInitializationInspection()
        )
    }

    fun `test optional fields`() {
        doInspectionTest(
            """
            struct Point {
                x: int?;
                y: int?;
            }

            fun foo() {
                <error descr="Fields 'x', 'y' missed in initialization">Point</error> {};
            }
            """.trimIndent(),
            TolkStructInitializationInspection()
        )
    }

    fun `test optional fields and non-optional`() {
        doInspectionTest(
            """
            struct Point {
                x: int?;
                y: int?;
                z: int;
            }

            fun foo() {
                <error descr="Fields 'x', 'y', 'z' missed in initialization">Point</error> {};
            }
            """.trimIndent(),
            TolkStructInitializationInspection()
        )
    }

    fun `test optional fields and non-optional 2`() {
        doInspectionTest(
            """
            struct Point {
                x: int?;
                y: int?;
                z: int;
            }

            fun foo() {
                <error descr="Fields 'x', 'y' missed in initialization">Point</error> { z: 10 };
            }
            """.trimIndent(),
            TolkStructInitializationInspection()
        )
    }

    fun `test never field can be omitted`() {
        doInspectionTest(
            """
            struct Point {
                x: int;
                y: never;
            }

            fun foo() {
                <error descr="Field 'x' missed in initialization">Point</error>{};
            }
            """.trimIndent(),
            TolkStructInitializationInspection()
        )
    }

    fun `test generic field with never default can be omitted`() {
        doInspectionTest(
            """
            struct Point<T=never> {
                x: int;
                y: T;
            }

            fun foo() {
                <error descr="Field 'x' missed in initialization">Point</error>{};
            }
            """.trimIndent(),
            TolkStructInitializationInspection()
        )
    }

    fun `test no error for struct with private fields`() {
        doInspectionTest(
            """
            struct Point {
                private x: int;
                private y: int;
            }

            fun foo() {
                Point {};
            }
            """.trimIndent(),
            TolkStructInitializationInspection()
        )
    }
}