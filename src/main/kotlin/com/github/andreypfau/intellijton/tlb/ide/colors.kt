package com.github.andreypfau.intellijton.tlb.ide

import com.github.andreypfau.intellijton.loadTextResource
import com.github.andreypfau.intellijton.tlb.TlbIcons
import com.github.andreypfau.intellijton.tlb.TlbLanguage
import com.intellij.openapi.editor.XmlHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import javax.swing.Icon
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors as Defaults

enum class TlbColor(
    displayName: String,
    default: TextAttributesKey
) {
    COMMENT("Comment", Defaults.LINE_COMMENT),
    DOCUMENTATION("Documentation", Defaults.DOC_COMMENT),

    BRACES("Braces", Defaults.BRACES),
    BRACKETS("Brackets", Defaults.BRACKETS),
    PARENTHESES("Parentheses", Defaults.PARENTHESES),
    SEMICOLON("Semicolon", Defaults.SEMICOLON),

    NUMBER("Number", Defaults.NUMBER),
    CONSTRUCTOR_NAME("Constructor name", Defaults.STATIC_METHOD),
    HEX_TAG("HEX Tag", Defaults.STATIC_FIELD),
    BINARY_TAG("Binary Tag", Defaults.STATIC_FIELD),
    FIELD_NAME("Field name", XmlHighlighterColors.HTML_ATTRIBUTE_NAME),
    IMPLICIT_FIELD_NAME("Implicit field name", TextAttributesKey.find("KOTLIN_TYPE_PARAMETER")),
    COMBINATOR_NAME("Combinator name", Defaults.KEYWORD),
    TYPE("Type", XmlHighlighterColors.HTML_ATTRIBUTE_VALUE)
    ;

    val textAttributesKey =
        TextAttributesKey.createTextAttributesKey("com.github.andreypfau.intellijton.Tlb.$name", default)
    val attributesDescriptor = AttributesDescriptor(displayName, textAttributesKey)
}


class TlbColorSettingsPage : ColorSettingsPage {
    private val DEMO_TEXT by lazy {
        loadTextResource(this, "colors/highlighter_example.tlb")
    }

    override fun getAttributeDescriptors(): Array<AttributesDescriptor> = ATTRIBUTE_DESCRIPTORS
    override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY
    override fun getDisplayName(): String = TlbLanguage.displayName
    override fun getIcon(): Icon = TlbIcons.FILE
    override fun getHighlighter(): SyntaxHighlighter = TlbSyntaxHighlighter
    override fun getDemoText(): String = DEMO_TEXT

    override fun getAdditionalHighlightingTagToDescriptorMap(): MutableMap<String, TextAttributesKey>? = null

    companion object {
        val ATTRIBUTE_DESCRIPTORS = TlbColor.values().map { it.attributesDescriptor }.toTypedArray()
    }
}