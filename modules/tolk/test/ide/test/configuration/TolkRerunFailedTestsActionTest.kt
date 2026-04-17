package org.ton.intellij.tolk.ide.test.configuration

import com.intellij.execution.testframework.sm.runner.SMTestProxy
import org.junit.Assert.assertEquals
import org.junit.Test

class TolkRerunFailedTestsActionTest {
    @Test
    fun `test rerun selection escapes regex metacharacters`() {
        val failedTest = SMTestProxy(
            "test teamcity '|[]",
            false,
            "tolk_qn:///tmp/tests/escape.test.tolk:test teamcity '|[]",
        )

        val selection = TolkRerunFailedTestsAction.buildRerunSelection(".", listOf(failedTest))

        assertEquals("/tmp/tests/escape.test.tolk", selection.target)
        assertEquals("^test teamcity '\\|\\[\\]$", selection.filterPattern)
    }

    @Test
    fun `test rerun selection narrows target only when all failed tests are in one file`() {
        val first = SMTestProxy("test alpha", false, "tolk_qn:///tmp/tests/a.test.tolk:test alpha")
        val second = SMTestProxy("test beta", false, "tolk_qn:///tmp/tests/b.test.tolk:test beta")

        val selection = TolkRerunFailedTestsAction.buildRerunSelection("/tmp/tests", listOf(first, second))

        assertEquals("/tmp/tests", selection.target)
        assertEquals("^(?:test alpha|test beta)$", selection.filterPattern)
    }

    @Test
    fun `test rerun selection ignores failed suites and keeps leaf tests only`() {
        val suite = SMTestProxy("wallet.test.tolk", true, null)
        val leaf = SMTestProxy("test transfer", false, "tolk_qn:///tmp/tests/wallet.test.tolk:test transfer")

        val selection = TolkRerunFailedTestsAction.buildRerunSelection("/tmp/tests", listOf(suite, leaf))

        assertEquals("/tmp/tests/wallet.test.tolk", selection.target)
        assertEquals("^test transfer$", selection.filterPattern)
    }

    @Test
    fun `test locator parses windows style file path from location url`() {
        val location =
            TolkTestLocator.parseLocationUrl("tolk_qn://C:/work/tests/wallet.test.tolk:test transfer")

        assertEquals("C:/work/tests/wallet.test.tolk", location?.filePath)
        assertEquals("test transfer", location?.functionName)
    }

    @Test
    fun `test rerun selection ignores root and file suite nodes when test locations are available`() {
        val root = SMTestProxy("[root]", true, null)
        val suite = SMTestProxy("counter.test.tolk", true, "file:///tmp/tests/counter.test.tolk")
        val first =
            SMTestProxy(
                "test deploy starts at zero",
                false,
                "tolk_qn:///tmp/tests/counter.test.tolk:test deploy starts at zero",
            )
        val second =
            SMTestProxy(
                "test increase counter",
                false,
                "tolk_qn:///tmp/tests/counter.test.tolk:test increase counter",
            )

        val selection =
            TolkRerunFailedTestsAction.buildRerunSelection(
                "/tmp/tests/counter.test.tolk",
                listOf(root, suite, first, second),
            )

        assertEquals("/tmp/tests/counter.test.tolk", selection.target)
        assertEquals("^(?:test deploy starts at zero|test increase counter)$", selection.filterPattern)
    }
}
