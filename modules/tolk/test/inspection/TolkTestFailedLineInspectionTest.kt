package org.ton.intellij.tolk.inspection

import com.intellij.execution.TestStateStorage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.ton.intellij.acton.runconfig.ComparisonFailure
import java.util.Date

class TolkTestFailedLineInspectionTest {
    @Test
    fun `message includes actual and expected from runtime comparison failure`() {
        val state =
            TestStateStorage.Record(
                0,
                Date(),
                0,
                0,
                null,
                "expect(<actual>).toEqual(<expected>)",
                "/tmp/tests/counter.test.tolk:11:5",
            )

        val message =
            TolkTestFailedLineInspection.buildProblemMessage(
                state,
                ComparisonFailure(actual = "0", expected = "1"),
            )

        assertEquals(
            "<html>Test failed: expect(&lt;actual&gt;).toEqual(&lt;expected&gt;)<br><pre>Actual:   0\nExpected: 1</pre></html>",
            message,
        )
    }

    @Test
    fun `message falls back to inline actual and expected in error message`() {
        val state =
            TestStateStorage.Record(
                0,
                Date(),
                0,
                0,
                null,
                """
                expect(<actual>).toEqual(<expected>)
                Actual:   0
                Expected: 1
                /tmp/tests/counter.test.tolk:11:5
                """.trimIndent(),
                "/tmp/tests/counter.test.tolk:11:5",
            )

        val message = TolkTestFailedLineInspection.buildProblemMessage(state, null)

        assertEquals(
            "<html>Test failed: expect(&lt;actual&gt;).toEqual(&lt;expected&gt;)<br><pre>Actual:   0\nExpected: 1</pre></html>",
            message,
        )
    }

    @Test
    fun `extract actual and expected ignores non comparison output`() {
        val details =
            TolkTestFailedLineInspection.extractActualExpectedDetails(
                "test failed with exit code 1",
                "/tmp/tests/counter.test.tolk:11:5",
            )

        assertNull(details)
    }
}
