package org.ton.intellij.tolk.doc

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import org.ton.intellij.tolk.ide.colors.TolkColor
import org.ton.intellij.util.doc.DocumentationUtils

object TolkDocumentationUtils : DocumentationUtils() {
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
}
