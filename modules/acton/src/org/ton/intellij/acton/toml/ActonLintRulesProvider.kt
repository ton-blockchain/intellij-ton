package org.ton.intellij.acton.toml

import com.google.gson.Gson
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import org.ton.intellij.acton.cli.ActonCommandLine
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentHashMap

object ActonLintRulesProvider {
    private val rulesCache = ConcurrentHashMap<String, CachedRules>()
    private const val CACHE_TTL_MS = 5 * 60 * 1000 // 5 minutes

    fun getLintRules(project: Project): List<LintRule> {
        val workingDir = project.guessProjectDir()?.toNioPath() ?: return emptyList()
        val cacheKey = workingDir.toString()

        val cached = rulesCache[cacheKey]
        if (cached != null && System.currentTimeMillis() - cached.timestamp < CACHE_TTL_MS) {
            return cached.rules
        }

        val commandLine = try {
            ActonCommandLine(
                command = "check",
                workingDirectory = workingDir,
                additionalArguments = listOf("--list-lint-rules"),
            ).toGeneralCommandLine(project)
        } catch (_: Exception) {
            return emptyList()
        }

        return try {
            val output = ApplicationManager.getApplication().executeOnPooledThread(Callable {
                val handler = CapturingProcessHandler(commandLine)
                handler.runProcess(5000)
            }).get()

            if (output.exitCode != 0) return emptyList()

            val rules = Gson().fromJson(output.stdout, Array<LintRule>::class.java).toList()
            if (rules.isNotEmpty()) {
                rulesCache[cacheKey] = CachedRules(rules, System.currentTimeMillis())
            }
            rules
        } catch (_: Exception) {
            emptyList()
        }
    }

    data class CachedRules(
        val rules: List<LintRule>,
        val timestamp: Long,
    )

    data class LintRule(
        val name: String,
        val description: String,
    )
}
