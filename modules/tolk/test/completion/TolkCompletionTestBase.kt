package org.ton.intellij.tolk.completion

import com.intellij.codeInsight.lookup.LookupElement
import org.intellij.lang.annotations.Language
import org.ton.intellij.tolk.TolkTestBase

open class TolkCompletionTestBase : TolkTestBase() {
    protected lateinit var completionFixture: TolkCompletionTestFixture

    override fun setUp() {
        super.setUp()
        completionFixture = TolkCompletionTestFixture(myFixture)
        completionFixture.setUp()
    }
    
    override fun tearDown() {
        completionFixture.tearDown()
        super.tearDown()
    }

    protected fun doFirstCompletion(
        @Language("Tolk") before: String,
        @Language("Tolk") after: String
    ) = completionFixture.doFirstCompletion(before, after)

    protected fun doSingleCompletion(
        @Language("Tolk") before: String,
        @Language("Tolk") after: String
    ) = completionFixture.doSingleCompletion(before, after)

    protected fun checkContainsCompletion(
        variant: String,
        @Language("Tolk") code: String,
        render: LookupElement.() -> String = { lookupString }
    ) = completionFixture.checkContainsCompletion(code, listOf(variant), render)

    protected fun checkContainsCompletion(
        variants: List<String>,
        @Language("Tolk") code: String,
        render: LookupElement.() -> String = { lookupString }
    ) = completionFixture.checkContainsCompletion(code, variants, render)

    protected fun checkContainsCompletionPrefixes(
        prefixes: List<String>,
        @Language("Tolk") code: String
    ) = completionFixture.checkContainsCompletionPrefixes(code, prefixes)

    protected fun checkCompletion(
        lookupString: String,
        @Language("Tolk") before: String,
        @Language("Tolk") after: String,
        completionChar: Char = '\n'
    ) = completionFixture.checkCompletion(lookupString, before, after, completionChar)

    protected fun checkCompletion(
        lookupString: String,
        tailText: String,
        @Language("Tolk") before: String,
        @Language("Tolk") after: String,
        completionChar: Char = '\n'
    ) = completionFixture.checkCompletion(lookupString, tailText, before, after, completionChar)

    protected fun checkNotContainsCompletion(
        variant: String,
        @Language("Tolk") code: String,
        render: LookupElement.() -> String = { lookupString }
    ) = completionFixture.checkNotContainsCompletion(code, setOf(variant), render)

    protected fun checkNotContainsCompletion(
        variants: Set<String>,
        @Language("Tolk") code: String,
        render: LookupElement.() -> String = { lookupString }
    ) = completionFixture.checkNotContainsCompletion(code, variants, render)

    protected fun checkNotContainsCompletion(
        variants: List<String>,
        @Language("Tolk") code: String,
        render: LookupElement.() -> String = { lookupString }
    ) {
        completionFixture.checkNotContainsCompletion(code, variants.toSet(), render)
    }

    protected open fun checkNoCompletion(@Language("Tolk") code: String) = completionFixture.checkNoCompletion(code)

    protected fun executeSoloCompletion() = completionFixture.executeSoloCompletion()
}
