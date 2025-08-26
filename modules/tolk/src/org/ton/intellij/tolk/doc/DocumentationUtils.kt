package org.ton.intellij.tolk.doc

import com.intellij.lang.documentation.DocumentationSettings
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.editor.richcopy.HtmlSyntaxInfoUtil
import io.ktor.util.*
import org.ton.intellij.tolk.ide.colors.TolkColor

object DocumentationUtils {
    private fun loadKey(key: TextAttributesKey): TextAttributes =
        EditorColorsManager.getInstance().globalScheme.getAttributes(key)!!

    val asKeyword = loadKey(TolkColor.KEYWORD.textAttributesKey)
    val asIdentifier = loadKey(DefaultLanguageHighlighterColors.IDENTIFIER)
    val asParameter = loadKey(TolkColor.PARAMETER.textAttributesKey)
    val asTypeParameter = loadKey(TolkColor.TYPE_PARAMETER.textAttributesKey)
    val asConstant = loadKey(TolkColor.CONSTANT.textAttributesKey)
    val asGlobalVariable = loadKey(TolkColor.GLOBAL_VARIABLE.textAttributesKey)
    val asString = loadKey(TolkColor.STRING.textAttributesKey)
    val asNumber = loadKey(TolkColor.NUMBER.textAttributesKey)
    val asField = loadKey(TolkColor.FIELD.textAttributesKey)
    val asParen = loadKey(TolkColor.PARENTHESES.textAttributesKey)
    val asComma = loadKey(TolkColor.COMMA.textAttributesKey)
    val asDot = loadKey(TolkColor.DOT.textAttributesKey)

    val asFunction = loadKey(TolkColor.FUNCTION.textAttributesKey)
    val asAnnotation = loadKey(TolkColor.ANNOTATION.textAttributesKey)
    val asStruct = loadKey(TolkColor.STRUCT.textAttributesKey)
    val asTypeAlias = loadKey(TolkColor.TYPE_ALIAS.textAttributesKey)
    val asPrimitive = loadKey(TolkColor.PRIMITIVE.textAttributesKey)

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

    fun StringBuilder.part(text: String?) {
        if (text.isNullOrEmpty()) {
            return
        }
        append(text)
        append(" ")
    }

    fun StringBuilder.part(text: String?, attrs: TextAttributes) {
        if (text == null) {
            return
        }
        colorize(text, attrs)
        append(" ")
    }

    fun StringBuilder.appendNotNull(text: String?) {
        if (text == null) {
            return
        }
        append(text)
    }

    fun StringBuilder.line(text: String?) {
        if (text == null) {
            return
        }
        append(text)
        append("\n")
    }

    fun StringBuilder.monospaced(text: String, attrs: TextAttributes? = null, noHtml: Boolean = false) {
        append("<code>")
        if (attrs != null) {
            colorize(text, attrs, noHtml)
        } else {
            append(text)
        }
        append("</code>")
    }

    fun colorize(text: String, attrs: TextAttributes, noHtml: Boolean = false): String {
        val sb = StringBuilder()
        sb.colorize(text, attrs, noHtml)
        return sb.toString()
    }
}
