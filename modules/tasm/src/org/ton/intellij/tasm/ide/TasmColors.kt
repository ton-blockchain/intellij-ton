package org.ton.intellij.tasm.ide

import com.intellij.openapi.editor.XmlHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import org.ton.intellij.tasm.TasmIcons
import org.ton.intellij.tasm.TasmLanguage
import org.ton.intellij.util.loadTextResource
import javax.swing.Icon
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors as Defaults

enum class TasmColor(
    displayName: String,
    default: TextAttributesKey,
) {
    COMMENT("Comment", Defaults.LINE_COMMENT),

    BRACES("Braces", Defaults.BRACES),
    BRACKETS("Brackets", Defaults.BRACKETS),
    ARROW("Arrow", Defaults.BRACKETS),

    STACK_REGISTER("Stack register", Defaults.INSTANCE_FIELD),
    CONTROL_REGISTER("Control register", Defaults.STATIC_METHOD),
    NUMBER("Number", Defaults.NUMBER),
    HEX_LITERAL("Hex literal", Defaults.NUMBER),
    BIN_LITERAL("Bin literal", Defaults.NUMBER),
    BOC_LITERAL("BoC literal", Defaults.NUMBER),
    KEYWORD("Keyword", Defaults.KEYWORD),

    INSTRUCTION("Instruction", XmlHighlighterColors.HTML_TAG)
    ;

    val textAttributesKey =
        TextAttributesKey.createTextAttributesKey("org.ton.intellij.tasm.$name", default)
    val attributesDescriptor = AttributesDescriptor(displayName, textAttributesKey)
}

class TasmColorSettingsPage : ColorSettingsPage {
    private val demo by lazy {
        loadTextResource(TasmColorSettingsPage::class.java, "colors/highlighter_example.tasm")
    }

    override fun getAdditionalHighlightingTagToDescriptorMap() = TasmColor.entries.associateBy({ it.name }, { it.textAttributesKey })
    override fun getAttributeDescriptors(): Array<AttributesDescriptor> = TasmColor.entries.map { it.attributesDescriptor }.toTypedArray()
    override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY
    override fun getDisplayName(): String = TasmLanguage.displayName
    override fun getIcon(): Icon = TasmIcons.FILE
    override fun getHighlighter(): SyntaxHighlighter = TasmSyntaxHighlighter
    override fun getDemoText(): String = demo
}
