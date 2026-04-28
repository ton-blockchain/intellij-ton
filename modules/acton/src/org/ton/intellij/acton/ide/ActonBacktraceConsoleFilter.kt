package org.ton.intellij.acton.ide

import com.intellij.codeInsight.hints.presentation.PresentationFactory
import com.intellij.codeInsight.hints.presentation.PresentationRenderer
import com.intellij.execution.filters.Filter
import com.intellij.execution.filters.Filter.ResultItem
import com.intellij.execution.impl.InlayProvider
import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorCustomElementRenderer
import org.ton.intellij.acton.runconfig.ActonCommandConfiguration
import java.awt.Cursor

class ActonBacktraceConsoleFilter(private val sourceConfiguration: ActonCommandConfiguration) : Filter {
    private var currentTestTarget: String? = null
    private var lastFailedTestName: String? = null

    override fun applyFilter(line: String, entireLength: Int): Filter.Result? {
        if (sourceConfiguration.command != "test") return null

        extractActonTestTarget(line)?.let { currentTestTarget = it }
        extractActonFailedTestName(line)?.let { lastFailedTestName = it }

        if (!RERUN_WITH_BACKTRACE_REGEX.containsMatchIn(line)) return null
        val lineStart = entireLength - line.length
        val visibleLineLength = line.trimEnd('\r', '\n').length
        val buttonOffset = if (visibleLineLength > 0) {
            lineStart + visibleLineLength
        } else {
            lineStart
        }
        return Filter.Result(
            listOf(
                ResultItem(lineStart, lineStart, null),
                ActonBacktraceInlay(buttonOffset, sourceConfiguration, lastFailedTestName, currentTestTarget),
            ),
        )
    }

    @Suppress("UnstableApiUsage")
    private class ActonBacktraceInlay(
        offset: Int,
        private val sourceConfiguration: ActonCommandConfiguration,
        private val failedTestName: String?,
        private val testTarget: String?,
    ) : ResultItem(offset, offset, null),
        InlayProvider {
        override fun createInlayRenderer(editor: Editor): EditorCustomElementRenderer {
            val factory = PresentationFactory(editor)
            val items = arrayOf(
                factory.smallScaledIcon(AllIcons.Actions.Execute),
                factory.smallText(" Re-run this test with backtrace full"),
            )
            val basePresentation = factory.referenceOnHover(factory.roundWithBackground(factory.seq(*items))) { _, _ ->
                ActonBacktraceRerunLauncher.launch(sourceConfiguration, failedTestName, testTarget)
            }
            val presentation = factory.withCursorOnHover(
                basePresentation,
                Cursor.getPredefinedCursor(Cursor.HAND_CURSOR),
            )
            return PresentationRenderer(presentation)
        }
    }
}

internal fun extractActonTestTarget(line: String): String? = TEST_SUITE_REGEX.find(line)?.groupValues?.get(1)

internal fun extractActonFailedTestName(line: String): String? {
    val name = FAILED_TEST_REGEX.find(line)?.groupValues?.get(1)?.trim() ?: return null
    if (name.matches(FAILED_SUMMARY_REGEX)) {
        return null
    }
    return name
}

private val RERUN_WITH_BACKTRACE_REGEX = Regex("""Re-run with --backtrace full[^\r\n]*""")
private val TEST_SUITE_REGEX = Regex("""^\s*>\s+(.+?\.tolk)\s+\(\d+\s+tests?\)\s*$""")
private val FAILED_TEST_REGEX = Regex("""^\s*✗\s+(.+?)(?:\s+\d+(?:\.\d+)?(?:ns|µs|ms|s))?\s*$""")
private val FAILED_SUMMARY_REGEX = Regex("""\d+\s+failed(?:,.*)?(?:\s+in\s+\d+\s+files?)?""")
