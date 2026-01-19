package org.ton.intellij.acton.cli

import com.intellij.util.execution.ParametersListUtil

sealed class ActonCommand(val name: String) {
    abstract fun getArguments(): List<String>

    data class Build(
        var contractId: String = "",
        var clearCache: Boolean = false,
        var outDir: String = "",
    ) : ActonCommand("build") {
        override fun getArguments(): List<String> = buildList {
            if (clearCache) add("--clear-cache")
            if (outDir.isNotBlank()) {
                add("--out-dir")
                add(outDir)
            }
            if (contractId.isNotBlank()) {
                add(contractId)
            }
        }
    }

    data class Script(
        var scriptPath: String = "",
        var clearCache: Boolean = false,
        var forkNet: String = "",
        var forkBlockNumber: String = "",
        var apiKey: String = "",
        var broadcast: Boolean = false,
        var broadcastNet: String = "",
        var explorer: String = "",
        var debug: Boolean = false,
        var debugPort: String = ""
    ) : ActonCommand("script") {
        override fun getArguments(): List<String> = buildList {
            if (clearCache) add("--clear-cache")
            if (forkNet.isNotBlank()) {
                add("--fork-net")
                add(forkNet)
            }
            if (forkBlockNumber.isNotBlank()) {
                add("--fork-block-number")
                add(forkBlockNumber)
            }
            if (apiKey.isNotBlank()) {
                add("--api-key")
                add(apiKey)
            }
            if (broadcast) {
                add("--broadcast")
                if (broadcastNet.isNotBlank()) {
                    add("--net")
                    add(broadcastNet)
                }
                if (explorer.isNotBlank()) {
                    add("--explorer")
                    add(explorer)
                }
            }
            if (debug) {
                add("--debug")
                if (debugPort.isNotBlank()) {
                    add("--debug-port")
                    add(debugPort)
                }
            }
            if (scriptPath.isNotBlank()) {
                add(scriptPath)
            }
        }
    }

    data class Test(
        var mode: TestMode = TestMode.DIRECTORY,
        var target: String = "",
        var functionName: String = "",
        var clearCache: Boolean = false,
    ) : ActonCommand("test") {
        enum class TestMode { FUNCTION, FILE, DIRECTORY }

        override fun getArguments(): List<String> = buildList {
            add("--reporter")
            add("console,teamcity")
            if (clearCache) add("--clear-cache")
            when (mode) {
                TestMode.FUNCTION -> {
                    if (functionName.isNotBlank()) {
                        add("--filter")
                        add(functionName.removeSurrounding("`"))
                    }
                    if (target.isNotBlank()) {
                        add(target)
                    }
                }
                TestMode.FILE -> add(target)
                TestMode.DIRECTORY -> add(target)
            }
        }
    }

    data class Run(
        var scriptName: String = "",
    ) : ActonCommand("run") {
        override fun getArguments(): List<String> = buildList {
            if (scriptName.isNotBlank()) {
                add(scriptName)
            }
        }
    }

    data class New(
        var path: String = ".",
        var projectName: String? = null,
        var description: String? = null,
        var template: String? = null,
        var license: String? = null,
    ) : ActonCommand("new") {
        override fun getArguments(): List<String> = buildList {
            add(path)
            projectName?.let {
                add("--name")
                add(it)
            }
            description?.let {
                add("--description")
                add(it)
            }
            template?.let {
                add("--template")
                add(it)
            }
            license?.let {
                add("--license")
                add(it)
            }
        }
    }

    data class Disasm(
        var bocFile: String = "",
        var string: String = "",
        var output: String = "",
        var showHashes: Boolean = false,
        var showOffsets: Boolean = false,
        var address: String = "",
        var apiKey: String = "",
        var net: String = "",
        var followLibraries: Boolean = false,
    ) : ActonCommand("disasm") {
        override fun getArguments(): List<String> = buildList {
            if (showHashes) add("--show-hashes")
            if (showOffsets) add("--show-offsets")
            if (string.isNotBlank()) {
                add("--string")
                add(string)
            }
            if (output.isNotBlank()) {
                add("--output")
                add(output)
            }
            if (address.isNotBlank()) {
                add("--address")
                add(address)
            }
            if (apiKey.isNotBlank()) {
                add("--api-key")
                add(apiKey)
            }
            if (net.isNotBlank()) {
                add("--net")
                add(net)
            }
            if (followLibraries) add("--follow-libraries")
            if (bocFile.isNotBlank()) {
                add(bocFile)
            }
        }
    }

    data class Compile(
        var path: String = "",
        var json: Boolean = false,
        var base64Only: Boolean = false,
        var boc: String? = null,
        var fift: String? = null,
        var sourceMap: String? = null,
        var clearCache: Boolean = false,
    ) : ActonCommand("compile") {
        override fun getArguments(): List<String> = buildList {
            if (json) add("--json")
            if (base64Only) add("--base64-only")
            boc?.let {
                add("--boc")
                add(it)
            }
            fift?.let {
                add("--fift")
                add(it)
            }
            sourceMap?.let {
                add("--source-map")
                add(it)
            }
            if (clearCache) add("--clear-cache")
            if (path.isNotBlank()) {
                add(path)
            }
        }
    }

    data class Check(
        var fix: Boolean = false,
        var json: Boolean = false,
    ) : ActonCommand("check") {
        override fun getArguments(): List<String> = buildList {
            if (fix) add("--fix")
            if (json) add("--json")
        }
    }

    data class InternalRegisterContract(
        var path: String = "",
        var id: String? = null,
    ) : ActonCommand("internal-register-contract") {
        override fun getArguments(): List<String> = buildList {
            id?.let {
                add("--id")
                add(it)
            }
            if (path.isNotBlank()) {
                add(path)
            }
        }
    }

    sealed class Wallet(val subcommand: String) : ActonCommand("wallet") {
        override fun getArguments(): List<String> = listOf(subcommand) + getSubcommandArguments()
        abstract fun getSubcommandArguments(): List<String>

        data class ListCmd(
            val balance: Boolean = false,
            val apiKey: String? = null,
            val json: Boolean = true
        ) : Wallet("list") {
            override fun getSubcommandArguments(): List<String> = buildList {
                if (balance) add("--balance")
                apiKey?.let {
                    add("--api-key")
                    add(it)
                }
                if (json) add("--json")
            }
        }

        data class New(
            val walletName: String? = null,
            val version: String? = null,
            val global: Boolean = false,
            val local: Boolean = false,
            val secure: Boolean? = null,
            val json: Boolean = true
        ) : Wallet("new") {
            override fun getSubcommandArguments(): List<String> = buildList {
                walletName?.let {
                    add("--name")
                    add(it)
                }
                version?.let {
                    add("--version")
                    add(it)
                }
                if (global) add("--global")
                if (local) add("--local")
                secure?.let {
                    add("--secure")
                    add(it.toString())
                }
                if (json) add("--json")
            }
        }

        data class Import(
            val walletName: String? = null,
            val mnemonics: List<String> = emptyList(),
            val version: String? = null,
            val global: Boolean = false,
            val local: Boolean = false,
            val secure: Boolean? = null,
            val json: Boolean = true
        ) : Wallet("import") {
            override fun getSubcommandArguments(): List<String> = buildList {
                walletName?.let {
                    add("--name")
                    add(it)
                }
                version?.let {
                    add("--version")
                    add(it)
                }
                if (global) add("--global")
                if (local) add("--local")
                secure?.let {
                    add("--secure")
                    add(it.toString())
                }
                if (json) add("--json")
                addAll(mnemonics)
            }
        }

        data class Airdrop(
            val walletName: String? = null,
            val faucetUrl: String? = null,
            val json: Boolean = true
        ) : Wallet("airdrop") {
            override fun getSubcommandArguments(): List<String> = buildList {
                walletName?.let {
                    add(it)
                }
                faucetUrl?.let {
                    add("--faucet-url")
                    add(it)
                }
                if (json) add("--json")
            }
        }
    }

    data class Custom(
        var command: String = "",
    ) : ActonCommand(command) {
        override fun getArguments(): List<String> = emptyList()
    }

    companion object {
        fun from(name: String, parameters: String): ActonCommand = when (name) {
            "build" -> {
                val args = ParametersListUtil.parse(parameters)
                val build = Build()
                var i = 0
                while (i < args.size) {
                    when (val arg = args[i]) {
                        "--clear-cache" -> build.clearCache = true
                        "--out-dir"     -> if (i + 1 < args.size) build.outDir = args[++i]
                        else            -> if (!arg.startsWith("-")) build.contractId = arg
                    }
                    i++
                }
                build
            }

            "disasm" -> {
                val args = ParametersListUtil.parse(parameters)
                val disasm = Disasm()
                var i = 0
                while (i < args.size) {
                    when (val arg = args[i]) {
                        "--show-hashes"     -> disasm.showHashes = true
                        "--show-offsets"    -> disasm.showOffsets = true
                        "--string", "-s"    -> if (i + 1 < args.size) disasm.string = args[++i]
                        "--output", "-o"    -> if (i + 1 < args.size) disasm.output = args[++i]
                        "--address"         -> if (i + 1 < args.size) disasm.address = args[++i]
                        "--api-key"         -> if (i + 1 < args.size) disasm.apiKey = args[++i]
                        "--net"             -> if (i + 1 < args.size) disasm.net = args[++i]
                        "--follow-libraries" -> disasm.followLibraries = true
                        else                -> if (!arg.startsWith("-")) disasm.bocFile = arg
                    }
                    i++
                }
                disasm
            }

            "compile" -> {
                val args = ParametersListUtil.parse(parameters)
                val compile = Compile()
                var i = 0
                while (i < args.size) {
                    when (val arg = args[i]) {
                        "--json"        -> compile.json = true
                        "--base64-only" -> compile.base64Only = true
                        "--boc"         -> if (i + 1 < args.size) compile.boc = args[++i]
                        "--fift"        -> if (i + 1 < args.size) compile.fift = args[++i]
                        "--source-map"  -> if (i + 1 < args.size) compile.sourceMap = args[++i]
                        "--clear-cache" -> compile.clearCache = true
                        else            -> if (!arg.startsWith("-")) compile.path = arg
                    }
                    i++
                }
                compile
            }

            "check" -> {
                val args = ParametersListUtil.parse(parameters)
                val check = Check()
                var i = 0
                while (i < args.size) {
                    when (args[i]) {
                        "--fix" -> check.fix = true
                        "--json" -> check.json = true
                        else     -> {} // ignore unknown arguments
                    }
                    i++
                }
                check
            }

            else    -> Custom("$name $parameters")
        }
    }
}
