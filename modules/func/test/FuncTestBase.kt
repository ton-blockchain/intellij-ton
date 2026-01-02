package org.ton.intellij.func

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.runner.RunWith

@RunWith(FuncJUnit4TestRunner::class)
abstract class FuncTestBase : BasePlatformTestCase() {
    override fun getTestName(lowercaseFirstLetter: Boolean): String {
        val name = super.getTestName(lowercaseFirstLetter)
        if (' ' in name) return name.trim().replace(" ", "_")
        return name.split("_").joinToString("_")
    }
}
