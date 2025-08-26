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

    val asKeyword get() = loadKey(TolkColor.KEYWORD.textAttributesKey)
    val asIdentifier get() = loadKey(DefaultLanguageHighlighterColors.IDENTIFIER)
    val asParameter get() = loadKey(TolkColor.PARAMETER.textAttributesKey)
    val asTypeParameter get() = loadKey(TolkColor.TYPE_PARAMETER.textAttributesKey)
    val asConstant get() = loadKey(TolkColor.CONSTANT.textAttributesKey)
    val asGlobalVariable get() = loadKey(TolkColor.GLOBAL_VARIABLE.textAttributesKey)
    val asString get() = loadKey(TolkColor.STRING.textAttributesKey)
    val asNumber get() = loadKey(TolkColor.NUMBER.textAttributesKey)
    val asField get() = loadKey(TolkColor.FIELD.textAttributesKey)
    val asParen get() = loadKey(TolkColor.PARENTHESES.textAttributesKey)
    val asComma get() = loadKey(TolkColor.COMMA.textAttributesKey)
    val asDot get() = loadKey(TolkColor.DOT.textAttributesKey)

    val asFunction get() = loadKey(TolkColor.FUNCTION.textAttributesKey)
    val asAnnotation get() = loadKey(TolkColor.ANNOTATION.textAttributesKey)
    val asStruct get() = loadKey(TolkColor.STRUCT.textAttributesKey)
    val asTypeAlias get() = loadKey(TolkColor.TYPE_ALIAS.textAttributesKey)
    val asPrimitive get() = loadKey(TolkColor.PRIMITIVE.textAttributesKey)

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
