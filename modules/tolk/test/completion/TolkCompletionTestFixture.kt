package org.ton.intellij.tolk.completion

import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import org.ton.intellij.tolk.InlineFile

class TolkCompletionTestFixture(
    fixture: CodeInsightTestFixture,
    private val defaultFileName: String = "main.tolk"
) : TolkCompletionTestFixtureBase<String>(fixture) {
    override fun prepare(code: String) {
        InlineFile(myFixture, code.trimIndent(), defaultFileName).withCaret()
    }

}
