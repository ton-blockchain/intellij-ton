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

            else    -> Custom("$name $parameters")
        }
    }
}
