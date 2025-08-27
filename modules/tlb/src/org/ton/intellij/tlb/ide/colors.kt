package org.ton.intellij.tlb.ide

import com.intellij.openapi.editor.XmlHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import org.ton.intellij.tlb.TlbIcons
import org.ton.intellij.tlb.TlbLanguage
import org.ton.intellij.util.loadTextResource
import javax.swing.Icon
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors as Defaults

enum class TlbColor(
    displayName: String,
    default: TextAttributesKey,
) {
    COMMENT("Comments//Line", Defaults.LINE_COMMENT),
    DOCUMENTATION("Comments//Documentation", Defaults.DOC_COMMENT),

    BRACES("Braces", Defaults.BRACES),
    BRACKETS("Brackets", Defaults.BRACKETS),
    PARENTHESES("Parentheses", Defaults.PARENTHESES),
    SEMICOLON("Semicolon", Defaults.SEMICOLON),
    OPERATION_SIGN("Operation Sign", Defaults.OPERATION_SIGN),

    NUMBER("Number", Defaults.NUMBER),
    CONSTRUCTOR_NAME("Type//Constructor name", Defaults.STATIC_METHOD),
    HEX_TAG("Type//Hex Tag", Defaults.STATIC_FIELD),
    BINARY_TAG("Type//Binary Tag", Defaults.STATIC_FIELD),
    FIELD_NAME("Type//Field name", Defaults.INSTANCE_FIELD),
    TYPE_PARAMETER("Type//Type parameter", TextAttributesKey.find("TYPE_PARAMETER_NAME_ATTRIBUTES")),
    RESULT_TYPE_NAME("Type//Result type name", Defaults.KEYWORD),
    IMPLICIT_FIELD_NAME("Type//Implicit field name", XmlHighlighterColors.HTML_ATTRIBUTE_VALUE),
    BUILTIN_TYPE("Builtin type", XmlHighlighterColors.HTML_ATTRIBUTE_VALUE),
    IDENTIFIER("Identifier",  XmlHighlighterColors.HTML_ATTRIBUTE_NAME),
    ;

    val textAttributesKey =
        TextAttributesKey.createTextAttributesKey("org.ton.intellij.Tlb.$name", default)
    val attributesDescriptor = AttributesDescriptor(displayName, textAttributesKey)
}

class TlbColorSettingsPage : ColorSettingsPage {
    private val DEMO_TEXT by lazy {
        loadTextResource(TlbColorSettingsPage::class.java, "colors/highlighter_example.tlb")
    }

    override fun getAdditionalHighlightingTagToDescriptorMap(): Map<String, TextAttributesKey> = TlbColor.entries.associateBy({ it.name }, { it.textAttributesKey })
    override fun getAttributeDescriptors(): Array<AttributesDescriptor> = TlbColor.entries.map { it.attributesDescriptor }.toTypedArray()
    override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY
    override fun getDisplayName(): String = TlbLanguage.displayName
    override fun getIcon(): Icon = TlbIcons.FILE
    override fun getHighlighter(): SyntaxHighlighter = TlbSyntaxHighlighter
    override fun getDemoText(): String = DEMO_TEXT
}
