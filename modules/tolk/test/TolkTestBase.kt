package org.ton.intellij.tolk

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.runner.RunWith

@RunWith(TolkJUnit4TestRunner::class)
abstract class TolkTestBase : BasePlatformTestCase() {
    override fun getTestName(lowercaseFirstLetter: Boolean): String {
        val name = super.getTestName(lowercaseFirstLetter)
        if (' ' in name) return name.trim().replace(" ", "_")
        return name.split("_").joinToString("_")
    }
}
