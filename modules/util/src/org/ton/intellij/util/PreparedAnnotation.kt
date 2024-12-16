package org.ton.intellij.util

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.util.InspectionMessage
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.util.NlsContexts.Tooltip
import com.intellij.openapi.util.TextRange
import com.intellij.xml.util.XmlStringUtil.escapeString

class PreparedAnnotation(
    val severity: ProblemHighlightType,
    @Suppress("UnstableApiUsage") @InspectionMessage val header: String,
    @Suppress("UnstableApiUsage") @Tooltip val description: String = "",
    val fixes: List<QuickFixWithRange> = emptyList(),
    val textAttributes: TextAttributesKey? = null,
) {
    val fullDescription: String get() = "<html>${escapeString(header)}<br>${escapeString(description)}</html>"
}

data class QuickFixWithRange(
    val fix: LocalQuickFix,
    val availabilityRange: TextRange?,
)

private fun listOfFixes(vararg fixes: LocalQuickFix?): List<QuickFixWithRange> =
    fixes.mapNotNull { if (it == null) null else QuickFixWithRange(it, null) }

private fun List<LocalQuickFix>.toQuickFixInfo(): List<QuickFixWithRange> = map { QuickFixWithRange(it, null) }
