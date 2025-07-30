package org.ton.intellij.fift.ide.documentation

import com.intellij.lang.documentation.DocumentationSettings
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.editor.richcopy.HtmlSyntaxInfoUtil
import io.ktor.util.*
import org.ton.intellij.fift.ide.FiftColor

object DocumentationUtils {
    private fun loadKey(key: TextAttributesKey): TextAttributes =
        EditorColorsManager.getInstance().globalScheme.getAttributes(key)!!

    val asAsmInstruction = loadKey(FiftColor.ASSEMBLY_INSTRUCTION.textAttributesKey)
    val asComment = loadKey(FiftColor.COMMENT.textAttributesKey)

    @Suppress("UnstableApiUsage")
    fun StringBuilder.colorize(text: String, attrs: TextAttributes, noHtml: Boolean = false) {
        if (noHtml) {
            append(text)
            return
        }

        HtmlSyntaxInfoUtil.appendStyledSpan(
            this, attrs, text.escapeHTML(),
            DocumentationSettings.getHighlightingSaturation(false)
        )
    }

    fun StringBuilder.line(text: String?) {
        if (text == null) {
            return
        }
        append(text)
        append("\n")
    }

    fun colorize(text: String, attrs: TextAttributes, noHtml: Boolean = false): String {
        val sb = StringBuilder()
        sb.colorize(text, attrs, noHtml)
        return sb.toString()
    }
}
