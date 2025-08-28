package org.ton.intellij.tolk.resolving

import com.intellij.openapi.editor.Caret
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.intellij.lang.annotations.Language
import org.ton.intellij.tolk.psi.TolkNamedElement

abstract class TolkResolvingTestBase : BasePlatformTestCase() {
    override fun getTestDataPath() = "src/test/resources/resolving"

    private var carets: MutableList<Caret>? = null
    private var caretIndex = 0

    protected fun assertReferencedTo(name: String, qualifier: Boolean = false) {
        val caret = carets!!.getOrNull(caretIndex++) ?: error("No more carets")
        val offset = caret.offset

        val ref = myFixture.file.findReferenceAt(offset)
        check(ref != null) { "No reference at caret" }

        val resolved = ref.resolve() as? TolkNamedElement
        check(resolved != null) { "Cannot resolve reference" }

        val kind = resolved.toString()
        val resolvedName = resolved.name
        val expected = "$kind $resolvedName"
        check(expected == name) { "Expected to resolve to $name, but got $expected" }
    }

    protected fun assertQualifiedReferencedTo(name: String) {
        assertReferencedTo(name, true)
    }

    protected fun assertUnresolved() {
        val caret = carets!!.getOrNull(caretIndex++) ?: error("No more carets")
        val offset = caret.offset

        val ref = myFixture.file.findReferenceAt(offset)
        val resolved = ref?.resolve()
        check(resolved == null) { "Not expected reference found at caret" }
    }

    protected fun file(path: String, @Language("Tolk") text: String) {
        val fileText = text.replace(CARET, CARET_ORIGINAL)
        myFixture.configureByText(path, fileText)
    }

    protected fun mainFile(path: String, @Language("Tolk") text: String) {
        file(path, text)
        carets = myFixture.editor.caretModel.allCarets
    }

    companion object {
        const val CARET = "/*caret*/"
        const val CARET_ORIGINAL = "<caret>"
    }
}