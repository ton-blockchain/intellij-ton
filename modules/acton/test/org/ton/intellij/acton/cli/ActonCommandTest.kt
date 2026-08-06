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

    @Test
    fun `script command supports broadcast to localnet`() {
        assertEquals(
            listOf("--net", "localnet", "script.tolk"),
            ActonCommand.Script(
                scriptPath = "script.tolk",
                broadcastNet = "localnet",
            ).getArguments(),
        )
    }

    @Test
    fun `localnet start command serializes options`() {
        assertEquals(
            listOf(
                "start",
                "--port",
                "3010",
                "--fork-net",
                "testnet",
                "--fork-block-number",
                "55000000",
                "--accounts",
                "deployer,user",
                "--rate-limit",
                "3",
            ),
            ActonCommand.Localnet.Start(
                port = 3010,
                forkNet = "testnet",
                forkBlockNumber = 55_000_000,
                accounts = listOf("deployer", "user"),
                rateLimit = 3,
            ).getArguments(),
        )
    }

    @Test
    fun `localnet status command serializes options`() {
        assertEquals(
            listOf("status", "--port", "3010", "--json"),
            ActonCommand.Localnet.Status(
                port = 3010,
                json = true,
            ).getArguments(),
        )
    }

    @Test
    fun `localnet airdrop command serializes options`() {
        assertEquals(
            listOf("airdrop", "UQAddress", "--amount", "25.0", "--port", "3010"),
            ActonCommand.Localnet.Airdrop(
                address = "UQAddress",
                amountTon = 25.0,
                port = 3010,
            ).getArguments(),
        )
    }
}
