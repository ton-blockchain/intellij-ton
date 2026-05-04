package org.ton.intellij.acton.cli

import org.junit.Assert.assertEquals
import org.junit.Test

class ActonCommandTest {
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
