package org.ton.intellij.tolk.completion
import org.intellij.lang.annotations.Language
import org.ton.intellij.tolk.type.TolkIntNTy
import org.ton.intellij.tolk.type.TolkTy
import org.ton.intellij.tolk.type.TolkTy.Companion.Cell
import org.ton.intellij.tolk.type.TolkTy.Companion.Slice
import org.ton.intellij.tolk.type.render

class TolkPrimitiveTypeCompletionTest : TolkCompletionTestBase() {
    fun `test return type`() = doTest("""
        fun foo(): /*caret*/
    """.trimIndent())

    fun `test type parameter`() = doTest("""
        struct Wrapper<T> { value: T }
        fun main() {
            val v: Wrapper</*caret*/> = 
        }
    """.trimIndent())

    private fun doTest(@Language("Tolk") text: String) {
        val primitiveTypes = TolkIntNTy.VALUES + TolkTy.Int + Cell + Slice + TolkTy.String + TolkTy.Builder + TolkTy.Coins
        val primitiveTypeNames = primitiveTypes.map { it.render() }
        checkContainsCompletion(primitiveTypeNames, text)
    }
}
