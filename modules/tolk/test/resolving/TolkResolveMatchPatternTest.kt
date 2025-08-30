package org.ton.intellij.tolk.resolving

open class TolkResolveMatchPatternTest : TolkResolvingTestBase() {
    fun `test type with same name as local variable`() {
        mainFile(
            "main.tolk",
            """
               type asdf = int;
                
               fun main() {
                   var asdf = 5;
                   return match (10) {
                       /*caret*/asdf => {}
                   };
               }
            """.trimIndent(),
        )

        assertReferencedTo("TYPE_DEF:asdf asdf")
    }

    fun `test type with same name as global variable`() {
        mainFile(
            "main.tolk",
            """
               type asdf = int;
       
               global asdf: int;
                
               fun main() {
                   return match (10) {
                       /*caret*/asdf => {}
                   };
               }
            """.trimIndent(),
        )

        assertReferencedTo("TYPE_DEF:asdf asdf")
    }
}
