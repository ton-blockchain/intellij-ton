package org.ton.intellij.acton.runconfig

import com.intellij.execution.testframework.sm.runner.SMTestProxy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ActonTestFailureStateServiceTest {
    @Test
    fun `stores comparison failure from test proxy`() {
        val service = ActonTestFailureStateService()
        val locationUrl = "tolk_qn:///tmp/tests/counter.test.tolk:test deploy starts at zero"
        val test = SMTestProxy("test deploy starts at zero", false, locationUrl)

        test.setTestComparisonFailed(
            "expect(<actual>).toEqual(<expected>)",
            "",
            "0",
            "1",
        )

        service.update(test)

        assertEquals(
            ComparisonFailure(actual = "0", expected = "1"),
            service.getComparisonFailure(locationUrl),
        )
    }

    @Test
    fun `removes stale comparison failure when latest failure has no diff`() {
        val service = ActonTestFailureStateService()
        val locationUrl = "tolk_qn:///tmp/tests/counter.test.tolk:test deploy starts at zero"
        val comparisonFailedTest = SMTestProxy("test deploy starts at zero", false, locationUrl)
        comparisonFailedTest.setTestComparisonFailed(
            "expect(<actual>).toEqual(<expected>)",
            "",
            "0",
            "1",
        )
        service.update(comparisonFailedTest)

        val plainFailedTest = SMTestProxy("test deploy starts at zero", false, locationUrl)
        plainFailedTest.setTestFailed("execution reverted", "", false)
        service.update(plainFailedTest)

        assertNull(service.getComparisonFailure(locationUrl))
    }
}
