package org.ton.intellij.tolk.completion

class TolkCompletionTest : TolkCompletionTestBase() {
    fun `test local variable`() = doSingleCompletion("""
        fun foo(quux: int) { qu/*caret*/ }
    """, """
        fun foo(quux: int) { quux/*caret*/ }
    """)

    fun `test function call zero args`() = doSingleCompletion("""
        fun foo() {}
        fun main() { fo/*caret*/ }
    """, """
        fun foo() {}
        fun main() { foo()/*caret*/ }
    """)

    fun `test function call one arg`() = doSingleCompletion("""
        fun foo(x: int) {}
        fun main() { fo/*caret*/ }
    """, """
        fun foo(x: int) {}
        fun main() { foo(/*caret*/) }
    """)

    fun `test function call with parens`() = doSingleCompletion("""
        fun foo() {}
        fun main() { fo/*caret*/() }
    """, """
        fun foo() {}
        fun main() { foo()/*caret*/ }
    """)

    fun `test function call with parens with arg`() = doSingleCompletion("""
        fun foo(x: int) {}
        fun main() { fo/*caret*/() }
    """, """
        fun foo(x: int) {}
        fun main() { foo(/*caret*/) }
    """)

    fun `test function call with parens overwrite`() = doSingleCompletion("""
        fun foo(x: int) {}
        fun main() { fo/*caret*/transmog() }
    """, """
        fun foo(x: int) {}
        fun main() { foo(/*caret*/)transmog() }
    """)

    fun `test local scope`() = checkNoCompletion("""
        fun main() {
            val x = spam/*caret*/;
            val spamlot = 42;
        }
    """.trimIndent())

    fun `test tuple field completion`() = checkContainsCompletion("1", """
        fun main() {
            val x = (0, 1);
            x./*caret*/
        }
    """.trimIndent())

    fun `test completion after tuple field expr`() = doSingleCompletion("""
        struct S { field: int }
        fun main() {
            val x = (0, S { field: 0 });
            x.1./*caret*/
        }
    """, """
        struct S { field: int }
        fun main() {
            val x = (0, S { field: 0 });
            x.1.field/*caret*/
        }
    """)

    fun `test lazy keyword`() = doSingleCompletion("""
        fun main() { laz/*caret*/ }
    """, """
        fun main() { lazy /*caret*/ }
    """)

    fun `test builtin keyword, plain function`() = doSingleCompletion("""
        fun main() buil/*caret*/
    """, """
        fun main() builtin/*caret*/
    """)

    fun `test builtin keyword, method`() = doSingleCompletion("""
        fun int.foo() buil/*caret*/
    """, """
        fun int.foo() builtin/*caret*/
    """)

    fun `test builtin keyword, get method`() = checkNoCompletion("""
        get fun foo() buil/*caret*/
    """)

    fun `test do-while loop, while keyword`() = doFirstCompletion("""
        fun foo() {
            do {
                
            } whi/*caret*/
        }
    """, """
        fun foo() {
            do {
                
            } while /*caret*/
        }
    """.trimIndent())

    fun `test val snippet`() = doFirstCompletion("""
        fun foo() {
            val/*caret*/
        }
    """, """
        fun foo() {
            val name = 0;/*caret*/
        }
    """.trimIndent())

    fun `test valt snippet`() = doFirstCompletion("""
        fun foo() {
            valt/*caret*/
        }
    """, """
        fun foo() {
            val name: int = 0;/*caret*/
        }
    """.trimIndent())

    fun `test var snippet`() = doFirstCompletion("""
        fun foo() {
            var/*caret*/
        }
    """, """
        fun foo() {
            var name = 0;/*caret*/
        }
    """.trimIndent())

    fun `test vart snippet`() = doFirstCompletion("""
        fun foo() {
            vart/*caret*/
        }
    """, """
        fun foo() {
            var name: int = 0;/*caret*/
        }
    """.trimIndent())

    fun `test if snippet`() = doFirstCompletion("""
        fun foo() {
            if/*caret*/
        }
    """, """
        fun foo() {
            if (true) {
                /*caret*/
            }
        }
    """.trimIndent())

    fun `test ife snippet`() = doFirstCompletion("""
        fun foo() {
            ife/*caret*/
        }
    """, """
        fun foo() {
            if (true) {
                
            } else {
                /*caret*/
            }
        }
    """.trimIndent())

    fun `test while snippet`() = doFirstCompletion("""
        fun foo() {
            while/*caret*/
        }
    """, """
        fun foo() {
            while (true) {
                /*caret*/
            }
        }
    """.trimIndent())

    fun `test do snippet`() = doFirstCompletion("""
        fun foo() {
            do/*caret*/
        }
    """, """
        fun foo() {
            do {
                /*caret*/
            } while (true);
        }
    """.trimIndent())

    fun `test repeat snippet`() = doFirstCompletion("""
        fun foo() {
            repeat/*caret*/
        }
    """, """
        fun foo() {
            repeat (10) {
                /*caret*/
            }
        }
    """.trimIndent())

    fun `test try snippet`() = doFirstCompletion("""
        fun foo() {
            try/*caret*/
        }
    """, """
        fun foo() {
            try {
                /*caret*/
            }
        }
    """.trimIndent())

    fun `test tryc snippet`() = doFirstCompletion("""
        fun foo() {
            tryc/*caret*/
        }
    """, """
        fun foo() {
            try {
                
            } catch (e) {
                /*caret*/
            }
        }
    """.trimIndent())

    fun `test match snippet`() = doFirstCompletion("""
        fun foo() {
            match/*caret*/
        }
    """, """
        fun foo() {
            match (true) {
                /*caret*/
            }
        }
    """.trimIndent())

    fun `test assert snippet`() = doFirstCompletion("""
        fun foo() {
            assert/*caret*/
        }
    """, """
        fun foo() {
            assert (false) throw 5;/*caret*/
        }
    """.trimIndent())

    fun `test throw snippet`() = doFirstCompletion("""
        fun foo() {
            throw/*caret*/
        }
    """, """
        fun foo() {
            throw 5;/*caret*/
        }
    """.trimIndent())

    fun `test throw snippet after assert`() = doFirstCompletion("""
        fun foo() {
            assert (false) throw/*caret*/
        }
    """, """
        fun foo() {
            assert (false) throw 5;/*caret*/
        }
    """.trimIndent())

    fun `test return, void type`() = doFirstCompletion("""
        fun foo() {
            return/*caret*/
        }
    """, """
        fun foo() {
            return;/*caret*/
        }
    """.trimIndent())

    fun `test return, int type`() = doFirstCompletion("""
        fun foo(): int {
            return/*caret*/
        }
    """, """
        fun foo(): int {
            return ;/*caret*/
        }
    """.trimIndent())

    fun `test return, bool type`() = doFirstCompletion("""
        fun foo(): bool {
            return/*caret*/
        }
    """, """
        fun foo(): bool {
            return ;/*caret*/
        }
    """.trimIndent())

    fun `test return, nullable type`() = doFirstCompletion("""
        fun foo(): slice? {
            return/*caret*/
        }
    """, """
        fun foo(): slice? {
            return ;/*caret*/
        }
    """.trimIndent())

    fun `test deprecated annotation`() = doFirstCompletion("""
        @de/*caret*/
        fun foo() {}
    """, """
        @deprecated("")/*caret*/
        fun foo() {}
    """.trimIndent())

    fun `test on_bounced_policy annotation`() = doFirstCompletion("""
        @on_bounced_polic/*caret*/
        fun main() {}
    """, """
        @on_bounced_policy("manual")/*caret*/
        fun main() {}
    """.trimIndent())

    fun `test no inline annotation for struct`() = checkNoCompletion("""
        @inl/*caret*/
        struct Foo {}
    """)

    fun `test no overflow1023_policy annotation for functions`() = checkNoCompletion("""
        @overflow1023_po/*caret*/
        fun foo() {}
    """)

    fun `test no on_bounced_policy annotation for non entry functions`() = checkNoCompletion("""
        @on_bounced_poli/*caret*/
        fun foo() {}
    """)

    fun `test no inline annotation for function with inline annotation`() = checkNoCompletion("""
        @inline
        @inl/*caret*/
        struct Foo {}
    """)

    fun `test postfix completion, arg`() = doFirstCompletion("""
        fun foo() {
            true.arg/*caret*/
        }
    """, """
        fun foo() {
            /*caret*/(true)
        }
    """.trimIndent())

    fun `test postfix completion, if`() = doFirstCompletion("""
        fun foo() {
            true.if/*caret*/
        }
    """, """
        fun foo() {
            if (true) {
                /*caret*/
            }
        }
    """.trimIndent())

    fun `test postfix completion, match`() = doFirstCompletion("""
        fun foo() {
            true.match/*caret*/
        }
    """, """
        fun foo() {
            match (true) {
                /*caret*/
            }
        }
    """.trimIndent())

    fun `test postfix completion, not`() = doFirstCompletion("""
        fun foo() {
            true.not/*caret*/
        }
    """, """
        fun foo() {
            !true/*caret*/
        }
    """.trimIndent())

    fun `test postfix completion, par`() = doFirstCompletion("""
        fun foo() {
            true.par/*caret*/
        }
    """, """
        fun foo() {
            (true)/*caret*/
        }
    """.trimIndent())

    fun `test postfix completion, val`() = doFirstCompletion("""
        fun foo() {
            true.val/*caret*/
        }
    """, """
        fun foo() {
            val name/*caret*/ = true;
        }
    """.trimIndent())

    fun `test postfix completion, var`() = doFirstCompletion("""
        fun foo() {
            true.var/*caret*/
        }
    """, """
        fun foo() {
            var name/*caret*/ = true;
        }
    """.trimIndent())

    fun `test type match arm completion, else`() = doFirstCompletion("""
        struct (0x7e8764ef) IncreaseCounter {
            queryId: uint64
            increaseBy: uint32
        }
        
        struct (0x3a752f06) ResetCounter {
            queryId: uint64
        }

        type AllowedMessage = IncreaseCounter | ResetCounter
        
        fun foo(msg: AllowedMessage) {
            match (msg) {
                IncreaseCounter => {}
                ResetCounter => {}
                /*caret*/
            }
        }
    """, """
        struct (0x7e8764ef) IncreaseCounter {
            queryId: uint64
            increaseBy: uint32
        }
        
        struct (0x3a752f06) ResetCounter {
            queryId: uint64
        }

        type AllowedMessage = IncreaseCounter | ResetCounter
        
        fun foo(msg: AllowedMessage) {
            match (msg) {
                IncreaseCounter => {}
                ResetCounter => {}
                else => {
                    /*caret*/
                }
            }
        }
    """)

    fun `test type match arm completion, no completion items`() = checkNoCompletion("""
        struct (0x7e8764ef) IncreaseCounter {
            queryId: uint64
            increaseBy: uint32
        }
        
        struct (0x3a752f06) ResetCounter {
            queryId: uint64
        }

        type AllowedMessage = IncreaseCounter | ResetCounter
        
        fun foo(msg: AllowedMessage) {
            match (msg) {
                IncreaseCounter => {}
                ResetCounter => {}
                else => {}
                /*caret*/
            }
        }
    """)

    fun `test type match arm completion, single item`() = doFirstCompletion("""
        struct (0x7e8764ef) IncreaseCounter {
            queryId: uint64
            increaseBy: uint32
        }
        
        struct (0x3a752f06) ResetCounter {
            queryId: uint64
        }

        type AllowedMessage = IncreaseCounter | ResetCounter
        
        fun foo(msg: AllowedMessage) {
            match (msg) {
                IncreaseCounter => {}
                /*caret*/
            }
        }
    """, """
        struct (0x7e8764ef) IncreaseCounter {
            queryId: uint64
            increaseBy: uint32
        }
        
        struct (0x3a752f06) ResetCounter {
            queryId: uint64
        }

        type AllowedMessage = IncreaseCounter | ResetCounter
        
        fun foo(msg: AllowedMessage) {
            match (msg) {
                IncreaseCounter => {}
                ResetCounter => {
                    /*caret*/
                }
            }
        }
    """)

    fun `test type match arm completion, fill all`() = doFirstCompletion("""
        struct (0x7e8764ef) IncreaseCounter {
            queryId: uint64
            increaseBy: uint32
        }
        
        struct (0x3a752f06) ResetCounter {
            queryId: uint64
        }

        type AllowedMessage = IncreaseCounter | ResetCounter
        
        fun foo(msg: AllowedMessage) {
            match (msg) {
                /*caret*/
            }
        }
    """, """
        struct (0x7e8764ef) IncreaseCounter {
            queryId: uint64
            increaseBy: uint32
        }
        
        struct (0x3a752f06) ResetCounter {
            queryId: uint64
        }

        type AllowedMessage = IncreaseCounter | ResetCounter
        
        fun foo(msg: AllowedMessage) {
            match (msg) {
                IncreaseCounter => {
                    /*caret*/
                }
                ResetCounter => {

                }
                else => {

                }
            }
        }
    """)

    fun `test value match arm completion, single item`() = doSingleCompletion("""
        const FOO = 100

        fun foo() {
            match (0) {
                FO/*caret*/
            }
        }
    """, """
        const FOO = 100

        fun foo() {
            match (0) {
                FOO/*caret*/
            }
        }
    """)

    fun `test fill all fields completion`() = doFirstCompletion("""
        struct Foo {
            a: int
            b: uint32
            c: bool
            d: slice
        }

        fun foo() {
            Foo{
                /*caret*/
            }
        }
    """, """
        struct Foo {
            a: int
            b: uint32
            c: bool
            d: slice
        }
        
        fun foo() {
            Foo{
                a: 0,
                b: 0,
                c: false,
                d: createEmptySlice(),/*caret*/
            }
        }
    """)

    fun `test @stdlib completion in empty import`() = doFirstCompletion("""
        import "/*caret*/"
    """, """
        import "@stdlib//*caret*/"
    """.trimIndent())

    fun `test @stdlib completion in non-empty import`() = checkNotContainsCompletion("@stdlib", """
        import "@stdlib//*caret*/"
    """)

    fun `test @stdlib completion in non-empty import2`() = checkNotContainsCompletion("@stdlib", """
        import "foo/*caret*/"
    """)

    fun `test storage snippet completion`() = doFirstCompletion("""
        storage/*caret*/
    """, """
        struct Storage {
            /*caret*/
        }

        fun Storage.load() {
            return Storage.fromCell(contract.getData());
        }

        fun Storage.save(self) {
            contract.setData(self.toCell());
        }
    """.trimIndent())

    fun `test no storage snippet completion in block`() = checkNotContainsCompletion("storage", """"
        fun foo() {
            storage/*caret*/
        }
    """)

    fun `test no storage snippet completion after fun`() = checkNotContainsCompletion("storage", """"
        fun storage/*caret*/
    """)

    fun `test onInternalMessage completion after fun keyword`() = doFirstCompletion("""
        fun onInternal/*caret*/
    """, """
        fun onInternalMessage(in: InMessage) {
            /*caret*/
        }
    """.trimIndent())

    fun `test onExternalMessage completion after fun keyword`() = doFirstCompletion("""
        fun onExternal/*caret*/
    """, """
        fun onExternalMessage(inMsg: slice) {
            /*caret*/
        }
    """.trimIndent())

    fun `test onInternalMessage completion after fun keyword with full declaration`() = doFirstCompletion("""
        fun onInternal/*caret*/(in: InMessage) {
        }
    """, """
        fun onInternalMessage/*caret*/(in: InMessage) {
        }
    """.trimIndent())

    fun `test no onInternalMessage completion in file with onInternalMessage`() = checkNotContainsCompletion("onInternalMessage", """"
        fun onInternalMessage(in: InMessage) {}        

        fun /*caret*/
    """)

    fun `test packToBuilder completion`() = doFirstCompletion("""
        type SnakeString = slice
        
        fun SnakeString.packT/*caret*/
    """, """
        type SnakeString = slice

        fun SnakeString.packToBuilder(self, mutate b: builder) {
            /*caret*/
        }
    """.trimIndent())

    fun `test unpackFromSlice completion`() = doFirstCompletion("""
        type SnakeString = slice

        fun SnakeString.unpack/*caret*/
    """, """
        type SnakeString = slice

        fun SnakeString.unpackFromSlice(mutate s: slice) {
            /*caret*/
        }
    """.trimIndent())

    fun `test unpackFromSlice completion with full declaration`() = doFirstCompletion("""
        type SnakeString = slice

        fun SnakeString.pack/*caret*/(self, mutate b: builder) {}
    """, """
        type SnakeString = slice

        fun SnakeString.packToBuilder/*caret*/(self, mutate b: builder) {}
    """.trimIndent())

    fun `test no unpackFromSlice completion for non alias type`() = checkNotContainsCompletion("unpackFromSlice", """
        struct Foo {}

        fun Foo.pack/*caret*/(self, mutate b: builder) {}
    """)

    fun `test no unpackFromSlice completion for builtin alias type`() = checkNotContainsCompletion("unpackFromSlice", """
        fun int.pack/*caret*/(self, mutate b: builder) {}
    """)

    fun `test backticked constant`() = doFirstCompletion("""
        const `aaaaaa` = 10

        fun main() {
            aaaaaa/*caret*/
        }
    """, """
        const `aaaaaa` = 10

        fun main() {
            `aaaaaa`/*caret*/
        }
    """.trimIndent())

    fun `test backticked global variable`() = doFirstCompletion("""
        global `aaaaaa`: int

        fun main() {
            aaaaaa/*caret*/
        }
    """, """
        global `aaaaaa`: int

        fun main() {
            `aaaaaa`/*caret*/
        }
    """.trimIndent())

    fun `test backticked type alias`() = doFirstCompletion("""
        type `aaaaaa` = int

        fun main() {
            val a: aaaaaa/*caret*/
        }
    """, """
        type `aaaaaa` = int

        fun main() {
            val a: `aaaaaa`/*caret*/
        }
    """.trimIndent())

    fun `test backticked function`() = doFirstCompletion("""
        fun `aaaaaa`() {}

        fun main() {
            aaaaaa/*caret*/
        }
    """, """
        fun `aaaaaa`() {}

        fun main() {
            `aaaaaa`()/*caret*/
        }
    """.trimIndent())

    fun `test backticked variable`() = doFirstCompletion("""
        fun main() {
            val `aaaaaa` = 10;
            aaaaaa/*caret*/
        }
    """, """
        fun main() {
            val `aaaaaa` = 10;
            `aaaaaa`/*caret*/
        }
    """.trimIndent())

//    fun `test caret navigation in self method`() = doSingleCompletion("""
//        struct Foo;
//        fun Foo.foo(self) {}
//        fun main() { Foo.fo/*caret*/ }
//    """, """
//        struct Foo;
//        fun Foo.foo(self) {}
//        fun main() { Foo.foo(/*caret*/) }
//    """.trimIndent())
}
