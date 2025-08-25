package org.ton.intellij.tolk.inspection

class TolkCallArgumentsCountMismatchInspectionTest : TolkInspectionTestBase() {

    fun `test too many arguments in function call`() {
        doInspectionTest(
            """
            fun add(a: int, b: int): int {
                return a + b;
            }

            fun main() {
                add(1, 2, <error descr="Too many arguments in call to 'add', expected 2, have 3">3</error>);
            }
            """.trimIndent(),
            TolkCallArgumentsCountMismatchInspection()
        )
    }

    fun `test too few arguments in function call`() {
        doInspectionTest(
            """
            fun add(a: int, b: int): int {
                return a + b;
            }

            fun main() {
                add(1<error descr="Too few arguments in call to 'add', expected 2, have 1">)</error>;
            }
            """.trimIndent(),
            TolkCallArgumentsCountMismatchInspection()
        )
    }

    fun `test correct number of arguments`() {
        doInspectionTest(
            """
            fun add(a: int, b: int): int {
                return a + b;
            }

            fun main() {
                add(1, 2);
            }
            """.trimIndent(),
            TolkCallArgumentsCountMismatchInspection()
        )
    }

    fun `test function with no parameters, too many arguments`() {
        doInspectionTest(
            """
            fun getValue(): int {
                return 42;
            }

            fun main() {
                getValue(<error descr="Too many arguments in call to 'getValue', expected 0, have 1">1</error>);
            }
            """.trimIndent(),
            TolkCallArgumentsCountMismatchInspection()
        )
    }

    fun `test function with no parameters, correct call`() {
        doInspectionTest(
            """
            fun getValue(): int {
                return 42;
            }

            fun main() {
                getValue();
            }
            """.trimIndent(),
            TolkCallArgumentsCountMismatchInspection()
        )
    }

    fun `test function with default parameters, all arguments provided`() {
        doInspectionTest(
            """
            fun greet(name: slice, prefix: slice = "Hello"): slice {
                return prefix + " " + name;
            }

            fun main() {
                greet("World", "Hi");
            }
            """.trimIndent(),
            TolkCallArgumentsCountMismatchInspection()
        )
    }

    fun `test function with default parameters, partial arguments`() {
        doInspectionTest(
            """
            fun greet(name: slice, prefix: slice = "Hello"): slice {
                return prefix + " " + name;
            }

            fun main() {
                greet("World");
            }
            """.trimIndent(),
            TolkCallArgumentsCountMismatchInspection()
        )
    }

    fun `test function with default parameters, too few arguments`() {
        doInspectionTest(
            """
            fun greet(name: slice, prefix: slice = "Hello"): slice {
                return prefix + " " + name;
            }

            fun main() {
                greet(<error descr="Too few arguments in call to 'greet', expected 2, have 0">)</error>;
            }
            """.trimIndent(),
            TolkCallArgumentsCountMismatchInspection()
        )
    }

    fun `test function with default parameters, too many arguments`() {
        doInspectionTest(
            """
            fun greet(name: slice, prefix: slice = "Hello"): slice {
                return prefix + " " + name;
            }

            fun main() {
                greet("World", "Hi", <error descr="Too many arguments in call to 'greet', expected 2, have 3">"Extra"</error>);
            }
            """.trimIndent(),
            TolkCallArgumentsCountMismatchInspection()
        )
    }

    fun `test multiple default parameters`() {
        doInspectionTest(
            """
            fun configure(host: slice, port: int = 8080, ssl: bool = false): slice {
                return host + ":" + port;
            }

            fun main() {
                configure("localhost");
                configure("localhost", 3000);
                configure("localhost", 3000, true);
            }
            """.trimIndent(),
            TolkCallArgumentsCountMismatchInspection()
        )
    }

    fun `test multiple default parameters, too few`() {
        doInspectionTest(
            """
            fun configure(host: slice, port: int = 8080, ssl: bool = false): slice {
                return host + ":" + port;
            }

            fun main() {
                configure(<error descr="Too few arguments in call to 'configure', expected 3, have 0">)</error>;
            }
            """.trimIndent(),
            TolkCallArgumentsCountMismatchInspection()
        )
    }

    fun `test multiple default parameters, too many`() {
        doInspectionTest(
            """
            fun configure(host: slice, port: int = 8080, ssl: bool = false): slice {
                return host + ":" + port;
            }

            fun main() {
                configure("localhost", 3000, true, <error descr="Too many arguments in call to 'configure', expected 3, have 4">"extra"</error>);
            }
            """.trimIndent(),
            TolkCallArgumentsCountMismatchInspection()
        )
    }

    fun `test instance method call, correct arguments`() {
        doInspectionTest(
            """
            struct Point {
                x: int;
                y: int;
            }

            fun Point.distance(self, other: Point): int {
                return abs(self.x) + abs(self.y);
            }

            fun main() {
                val p1 = Point{x: 0, y: 0};
                val p2 = Point{x: 3, y: 4};
                p1.distance(p2);
            }
            """.trimIndent(),
            TolkCallArgumentsCountMismatchInspection()
        )
    }

    fun `test instance method call, too many arguments`() {
        doInspectionTest(
            """
            struct Point {
                x: int;
                y: int;
            }

            fun Point.distance(self, other: Point): int {
                return abs(self.x) + abs(self.y);
            }

            fun main() {
                val p1 = Point{x: 0, y: 0};
                val p2 = Point{x: 3, y: 4};
                p1.distance(p2, <error descr="Too many arguments in call to 'distance', expected 1, have 2">p1</error>);
            }
            """.trimIndent(),
            TolkCallArgumentsCountMismatchInspection()
        )
    }

    fun `test instance method call, too few arguments`() {
        doInspectionTest(
            """
            struct Point {
                x: int;
                y: int;
            }

            fun Point.distance(self, other: Point): int {
                return abs(self.x) + abs(self.y);
            }

            fun main() {
                val p1 = Point{x: 0, y: 0};
                p1.distance(<error descr="Too few arguments in call to 'distance', expected 1, have 0">)</error>;
            }
            """.trimIndent(),
            TolkCallArgumentsCountMismatchInspection()
        )
    }

    fun `test static method call via struct, correct arguments`() {
        doInspectionTest(
            """
            struct Math {
            }

            fun Math.add(a: int, b: int): int {
                return a + b;
            }

            fun main() {
                Math.add(1, 2);
            }
            """.trimIndent(),
            TolkCallArgumentsCountMismatchInspection()
        )
    }

    fun `test static method call via struct, too many arguments`() {
        doInspectionTest(
            """
            struct Math {
            }

            fun Math.add(a: int, b: int): int {
                return a + b;
            }

            fun main() {
                Math.add(1, 2, <error descr="Too many arguments in call to 'add', expected 2, have 3">3</error>);
            }
            """.trimIndent(),
            TolkCallArgumentsCountMismatchInspection()
        )
    }

    fun `test method with default parameters, instance call`() {
        doInspectionTest(
            """
            struct Logger {
                level: int;
            }

            fun Logger.log(self, message: slice, level: int = 1): void {
                // log implementation
            }

            fun main() {
                val logger = Logger{level: 0};
                logger.log("Info");
                logger.log("Error", 2);
            }
            """.trimIndent(),
            TolkCallArgumentsCountMismatchInspection()
        )
    }

    fun `test method with default parameters, too many arguments`() {
        doInspectionTest(
            """
            struct Logger {
                level: int;
            }

            fun Logger.log(self, message: slice, level: int = 1): void {
                // log implementation
            }

            fun main() {
                val logger = Logger{level: 0};
                logger.log("Info", 2, <error descr="Too many arguments in call to 'log', expected 2, have 3">"extra"</error>);
            }
            """.trimIndent(),
            TolkCallArgumentsCountMismatchInspection()
        )
    }

    fun `test nested function calls`() {
        doInspectionTest(
            """
            fun add(a: int, b: int): int {
                return a + b;
            }

            fun multiply(a: int, b: int): int {
                return a * b;
            }

            fun main() {
                add(multiply(2, 3), multiply(4<error descr="Too few arguments in call to 'multiply', expected 2, have 1">)</error>);
            }
            """.trimIndent(),
            TolkCallArgumentsCountMismatchInspection()
        )
    }

    fun `test complex expression in function call`() {
        doInspectionTest(
            """
            fun calculate(a: int, b: int, c: int): int {
                return a + b * c;
            }

            fun getValue(): int {
                return 10;
            }

            fun main() {
                calculate(1 + 2, getValue(), 5, <error descr="Too many arguments in call to 'calculate', expected 3, have 4">6</error>);
            }
            """.trimIndent(),
            TolkCallArgumentsCountMismatchInspection()
        )
    }

    fun `test static method call with generic`() {
        doInspectionTest(
            """
            struct Iterator<T = int> {
                data: T;
            }

            fun Iterator<T>.new(): Iterator<T> {
                return Iterator{data: null as T};
            }

            fun main() {
                Iterator<int>.new();
            }
            """.trimIndent(),
            TolkCallArgumentsCountMismatchInspection()
        )
    }

    fun `test static method call with generic, too many arguments`() {
        doInspectionTest(
            """
            struct Iterator<T = int> {
                data: T;
            }

            fun Iterator<T>.new(): Iterator<T> {
                return Iterator{data: null as T};
            }

            fun main() {
                Iterator<int>.new(<error descr="Too many arguments in call to 'new', expected 0, have 2">1, 2</error>);
            }
            """.trimIndent(),
            TolkCallArgumentsCountMismatchInspection()
        )
    }

    fun `test instance method call as static, too many arguments`() {
        doInspectionTest(
            """
            struct Iterator<T = int> {
                data: T;
            }

            fun Iterator<T>.new(): Iterator<T> {
                return Iterator{data: null as T};
            }

            fun Iterator<T>.data(self): T {
                return self.data;
            }

            fun main() {
                val it = Iterator<int>.new(<error descr="Too many arguments in call to 'new', expected 0, have 2">1, 2</error>);
                Iterator<int>.data(it);
            }
            """.trimIndent(),
            TolkCallArgumentsCountMismatchInspection()
        )
    }

    fun `test instance method call as static, too few arguments`() {
        doInspectionTest(
            """
            struct Iterator<T = int> {
                data: T;
            }

            fun Iterator<T>.new(): Iterator<T> {
                return Iterator{data: null as T};
            }

            fun Iterator<T>.getData(self): T {
                return self.data;
            }

            fun main() {
                val it = Iterator<int>.new();
                Iterator<int>.getData(<error descr="Too few arguments in call to 'getData', expected 1, have 0">)</error>;
            }
            """.trimIndent(),
            TolkCallArgumentsCountMismatchInspection()
        )
    }

    fun `test instance method call as static, too many arguments 2`() {
        doInspectionTest(
            """
            struct Iterator<T = int> {
                data: T;
            }

            fun Iterator<T>.new(): Iterator<T> {
                return Iterator{data: null as T};
            }

            fun Iterator<T>.getData(self): T {
                return self.data;
            }

            fun main() {
                val it = Iterator<int>.new();
                Iterator<int>.getData(it, <error descr="Too many arguments in call to 'getData', expected 1, have 3">1, 2</error>);
            }
            """.trimIndent(),
            TolkCallArgumentsCountMismatchInspection()
        )
    }
}
