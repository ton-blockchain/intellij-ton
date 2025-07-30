package org.ton.intellij.asm.ide

import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.XmlHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors as Defaults

enum class AsmColor(
    displayName: String,
    default: TextAttributesKey,
) {
    BAD_CHARACTER("Bad character", HighlighterColors.BAD_CHARACTER),
    COMMENT("Comment", Defaults.LINE_COMMENT),

    NUMBER("Number", Defaults.NUMBER),
    STACK_REGISTER("Stack register", Defaults.CONSTANT),
    CONTROL_REGISTER("Control register", Defaults.CONSTANT),
    INSTRUCTION("Assembly instruction", XmlHighlighterColors.HTML_TAG)
    ;

    val textAttributesKey =
        TextAttributesKey.createTextAttributesKey("org.ton.intellij.fift.$name", default)
    val attributesDescriptor = AttributesDescriptor(displayName, textAttributesKey)
}
