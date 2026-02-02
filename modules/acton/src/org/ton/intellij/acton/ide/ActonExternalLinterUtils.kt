package org.ton.intellij.acton.ide

import com.intellij.codeInsight.daemon.HighlightDisplayKey
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.impl.HighlightInfoType
import com.intellij.codeInspection.SuppressIntentionAction
import com.intellij.codeInspection.SuppressIntentionActionFromFix.convertBatchToSuppressIntentionActions
import com.intellij.execution.configuration.EnvironmentVariablesData
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.AnyPsiChangeListener
import com.intellij.psi.impl.PsiManagerImpl
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.util.execution.ParametersListUtil
import com.intellij.util.messages.MessageBus
import org.apache.commons.lang3.StringEscapeUtils
import org.jetbrains.annotations.Nls
import org.ton.intellij.acton.ActonBundle
import org.ton.intellij.acton.cli.ActonCommand
import org.ton.intellij.acton.cli.ActonCommandLine
import org.ton.intellij.acton.ide.ActonExternalLinterFilteredMessage.Companion.filterMessage
import org.ton.intellij.acton.settings.externalLinterSettings
import java.nio.file.Path
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

object ActonExternalLinterUtils {
    private val LOG = logger<ActonExternalLinterUtils>()

    private data class CacheKey(
        val workingDirectory: Path,
        val additionalArguments: String,
        val envs: Map<String, String>,
        val isPassParentEnvs: Boolean,
    )

    private val CACHE_KEY =
        Key.create<ConcurrentHashMap<CacheKey, Pair<Long, Lazy<ActonExternalLinterResult?>>>>("ActonExternalLinterCache")

    fun checkLazily(
        project: Project,
        workingDirectory: Path,
        document: Document?,
    ): Lazy<ActonExternalLinterResult?> {
        val modificationTracker = PsiModificationTracker.getInstance(project)
        val modificationCount = modificationTracker.modificationCount
        val settings = project.externalLinterSettings
        val key = CacheKey(workingDirectory, settings.additionalArguments, settings.envs, settings.isPassParentEnvs)

        val cache = project.getUserData(CACHE_KEY) ?: ConcurrentHashMap<CacheKey, Pair<Long, Lazy<ActonExternalLinterResult?>>>().also {
            project.putUserData(CACHE_KEY, it)
        }

        val cached = cache[key]
        if (cached != null && cached.first == modificationCount) {
            return cached.second
        }

        val newLazyResult = lazy {
            checkWrapped(project, workingDirectory, document)
        }

        cache[key] = modificationCount to newLazyResult
        return newLazyResult
    }

    private fun checkWrapped(
        project: Project,
        workingDirectory: Path,
        document: Document?,
    ): ActonExternalLinterResult? {
        val application = ApplicationManager.getApplication()
        if (application.isDispatchThread) {
            saveDocument(document)
        } else {
            ApplicationManager.getApplication().invokeAndWait({
                saveDocument(document)
            }, ModalityState.nonModal())
        }

        return try {
            check(project, workingDirectory)
        } catch (e: Exception) {
            LOG.warn("Cannot check project", e)
            return null
        } finally {
        }
    }

    private fun saveDocument(document: Document?) {
        if (document != null) {
            FileDocumentManager.getInstance().saveDocument(document)
        } else {
            saveAllDocumentsAsTheyAre()
        }
    }

    private fun check(
        project: Project,
        workingDirectory: Path,
    ): ActonExternalLinterResult? {
        ProgressManager.checkCanceled()
        val started = Instant.now()

        val command = ActonCommand.Check(fix = false, json = true)
        val settings = project.externalLinterSettings
        val commandLine = ActonCommandLine(
            workingDirectory = workingDirectory,
            command = command.name,
            additionalArguments = command.getArguments() + ParametersListUtil.parse(settings.additionalArguments),
            environmentVariables = EnvironmentVariablesData.create(settings.envs, settings.isPassParentEnvs)
        ).toGeneralCommandLine(project) ?: return null

        val handler = CapturingProcessHandler(commandLine)
        val output = handler.runProcess(10000)
        val finish = Instant.now()
        val executionTime = finish.toEpochMilli() - started.toEpochMilli()

        if (output.exitCode != 0) {
            val errorMessage = "Acton check failed with exit code ${output.exitCode}: ${output.stderr}"
            LOG.warn(errorMessage)
            return ActonExternalLinterResult(
                commandOutput = "",
                executionTime = executionTime,
                error = errorMessage
            )
        }

        return ActonExternalLinterResult(
            commandOutput = output.stdout,
            executionTime = executionTime
        )
    }
}

data class ActonExternalLinterFilteredMessage(
    val severity: HighlightSeverity,
    val textRange: TextRange,
    @Nls val message: String,
    @Nls val htmlTooltip: String,
    val quickFixes: List<ActonApplySuggestionFix>,
    val suppressionFixes: List<SuppressIntentionAction>,
) {
    companion object {
        fun filterMessage(file: PsiFile, document: Document, diagnostic: ActonDiagnostic): ActonExternalLinterFilteredMessage? {
            val severity = when (diagnostic.severity) {
                "error"   -> HighlightSeverity.ERROR
                "warning" -> HighlightSeverity.WEAK_WARNING
                else      -> HighlightSeverity.INFORMATION
            }

            if (diagnostic.file != file.virtualFile.path) {
                return null
            }

            // Find primary annotation
            val primaryAnnotation = diagnostic.annotations.firstOrNull { it.isPrimary } ?: return null

            val textRange = primaryAnnotation.range.toTextRange(document) ?: return null

            @NlsSafe val tooltip = buildString {
                append(StringEscapeUtils.escapeHtml4(diagnostic.message))

                val additionalMessages = diagnostic.annotations
                    .filter { !it.isPrimary && it.message != null }
                    .map { StringEscapeUtils.escapeHtml4(it.message!!) }

                if (additionalMessages.isNotEmpty()) {
                    append(additionalMessages.joinToString(prefix = "<br>", separator = "<br>"))
                }
            }

            val quickFixes = diagnostic.fixes
                .filter { it.applicability == ActonApplicability.Auto }
                .mapNotNull { fix ->
                    ActonApplySuggestionFix.fromFix(fix, document)
                }

            val ruleName = diagnostic.name ?: "unknown"
            val actions = arrayOf(ActonSuppressLinterFix(ruleName), ActonSuppressLinterFix("all"))
            val suppressionOptions = convertBatchToSuppressIntentionActions(actions).toList()

            return ActonExternalLinterFilteredMessage(
                severity,
                textRange,
                diagnostic.message,
                tooltip,
                quickFixes,
                suppressionOptions
            )
        }
    }
}

private fun ActonRange.toTextRange(document: Document): TextRange? {
    val startLine = start.line
    val endLine = end.line
    
    if (startLine !in 0 until document.lineCount || endLine !in 0 until document.lineCount) {
        return null
    }
    
    val startOffset = (document.getLineStartOffset(startLine) + start.character).coerceIn(0, document.textLength)
    val endOffset = (document.getLineStartOffset(endLine) + end.character).coerceIn(0, document.textLength)

    if (startOffset > endOffset) {
        return null
    }

    return TextRange(startOffset, endOffset)
}

private fun saveAllDocumentsAsTheyAre() {
    FileDocumentManager.getInstance().saveAllDocuments()
}

fun MessageBus.createDisposableOnAnyPsiChange(): Disposable {
    val disposable = Disposer.newDisposable("Dispose on PSI change")
    connect(disposable).subscribe(
        PsiManagerImpl.ANY_PSI_CHANGE_TOPIC,
        object : AnyPsiChangeListener {
            override fun beforePsiChanged(isPhysical: Boolean) {
                if (isPhysical) {
                    Disposer.dispose(disposable)
                }
            }
        }
    )
    return disposable
}

fun MutableList<HighlightInfo>.addHighlightsForFile(
    file: PsiFile,
    annotationResult: ActonExternalLinterResult,
) {
    val doc = file.viewProvider.document
        ?: error("Can't find document for $file in external linter")

    if (annotationResult.error != null) {
        val highlightBuilder = HighlightInfo.newHighlightInfo(HighlightInfoType.WEAK_WARNING)
            .severity(HighlightSeverity.WEAK_WARNING)
            .description(annotationResult.error)
            .range(TextRange(0, 0))
        highlightBuilder.create()?.let(::add)
    }

    val filteredMessages = annotationResult.diagnostics
        .mapNotNull { diagnostic -> filterMessage(file, doc, diagnostic) }
        .distinct()

    val displayName = ActonBundle.message("settings.acton.external.linter.name")
    val key = HighlightDisplayKey.findOrRegister(ACTON_EXTERNAL_LINTER_ID, displayName)

    for (message in filteredMessages) {
        val highlightBuilder = HighlightInfo.newHighlightInfo(convertSeverity(message.severity))
            .severity(message.severity)
            .description(message.message)
            .escapedToolTip(message.htmlTooltip)
            .range(message.textRange)
            .needsUpdateOnTyping(true)

        if (message.quickFixes.isEmpty()) {
            for (suppressionFix in message.suppressionFixes) {
                highlightBuilder.registerFix(suppressionFix, null, null, message.textRange, key)
            }
        } else {
            for (fix in message.quickFixes) {
                highlightBuilder.registerFix(fix, message.suppressionFixes, null, message.textRange, key)
            }
        }

        highlightBuilder.create()?.let(::add)
    }
}

private fun convertSeverity(severity: HighlightSeverity): HighlightInfoType = when (severity) {
    HighlightSeverity.ERROR                           -> HighlightInfoType.ERROR
    HighlightSeverity.WARNING                         -> HighlightInfoType.WARNING
    HighlightSeverity.WEAK_WARNING                    -> HighlightInfoType.WEAK_WARNING
    HighlightSeverity.GENERIC_SERVER_ERROR_OR_WARNING -> HighlightInfoType.GENERIC_WARNINGS_OR_ERRORS_FROM_SERVER
    else                                              -> HighlightInfoType.INFORMATION
}

private const val ACTON_EXTERNAL_LINTER_ID: String = "ActonExternalLinterOptions"
