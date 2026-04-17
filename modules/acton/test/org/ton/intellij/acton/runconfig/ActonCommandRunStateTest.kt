package org.ton.intellij.acton.runconfig

import com.intellij.execution.runners.ExecutionEnvironment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.ton.intellij.acton.cli.ActonCommand

class ActonCommandRunStateTest {
    @Test
    fun `test applies rerun override once and clears it`() {
        val environment = ExecutionEnvironment()
        val baseCommand = ActonCommand.Test(
            mode = ActonCommand.Test.TestMode.DIRECTORY,
            target = ".",
            useColors = true,
        )
        environment.putUserData(
            ActonCommandRunState.TEST_COMMAND_OVERRIDE_KEY,
            TestCommandOverride(
                mode = ActonCommand.Test.TestMode.FUNCTION,
                target = "tests/counter.test.tolk",
                functionName = "^(?:deploy starts at zero|increase counter)$",
            ),
        )

        val rerunCommand = ActonCommandRunState.createTestCommand(baseCommand, environment, null)
        val regularCommand = ActonCommandRunState.createTestCommand(baseCommand, environment, null)

        assertEquals(ActonCommand.Test.TestMode.FUNCTION, rerunCommand.mode)
        assertEquals("tests/counter.test.tolk", rerunCommand.target)
        assertEquals("^(?:deploy starts at zero|increase counter)$", rerunCommand.functionName)
        assertNull(environment.getUserData(ActonCommandRunState.TEST_COMMAND_OVERRIDE_KEY))
        assertEquals(baseCommand, regularCommand)
    }
}
