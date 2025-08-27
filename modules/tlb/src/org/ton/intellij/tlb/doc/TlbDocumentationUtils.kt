package org.ton.intellij.tlb.doc

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import org.ton.intellij.tlb.ide.TlbColor
import org.ton.intellij.util.doc.DocumentationUtils

object TlbDocumentationUtils : DocumentationUtils() {
    val asIdentifier get() = loadKey(DefaultLanguageHighlighterColors.IDENTIFIER)
    val asResultType get() = loadKey(TlbColor.RESULT_TYPE_NAME.textAttributesKey)
    val asTypeParameter get() = loadKey(TlbColor.TYPE_PARAMETER.textAttributesKey)
    val asNumber get() = loadKey(TlbColor.NUMBER.textAttributesKey)
    val asBinaryTag get() = loadKey(TlbColor.BINARY_TAG.textAttributesKey)
    val asBuiltinType get() = loadKey(TlbColor.BUILTIN_TYPE.textAttributesKey)
    val asKeyword get() = loadKey(DefaultLanguageHighlighterColors.KEYWORD)
}
