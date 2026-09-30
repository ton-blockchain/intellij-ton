package org.ton.intellij.acton.cli

import org.junit.Assert.assertEquals
import org.junit.Test

class ActonCommandTest {
    @Test
    fun `disasm batches function names in one command`() {
        val command = ActonCommand.Disasm(
            bocFile = "contracts/token wallet.tolk",
            json = true,
            functions = (1..10).map { "function$it" },
        )
        val arguments = listOf("--json") +
            (1..10).flatMap { listOf("--function", "function$it") } +
            listOf("contracts/token wallet.tolk")

        assertEquals(arguments, command.getArguments())
        assertEquals(
            command,
            ActonCommand.from("disasm", com.intellij.util.execution.ParametersListUtil.join(arguments)),
        )
    }

    @Test
    fun `init command includes create dapp flag`() {
        assertEquals(
            listOf("--create-dapp"),
            ActonCommand.Init(createDapp = true).getArguments(),
        )
    }

    @Test
    fun `init command keeps stdlib and create dapp flags`() {
        assertEquals(
            listOf("--stdlib-only", "--create-dapp"),
            ActonCommand.Init(stdlibOnly = true, createDapp = true).getArguments(),
        )
    }

    @Test
    fun `from parses init create dapp flag`() {
        assertEquals(
            ActonCommand.Init(createDapp = true),
            ActonCommand.from("init", "--create-dapp"),
        )
    }

    @Test
    fun `test command includes full backtrace flag`() {
        assertEquals(
            listOf("--reporter", "console,teamcity", "--backtrace", "full", "."),
            ActonCommand.Test(
                target = ".",
                backtraceFull = true,
            ).getArguments(),
        )
    }
}
