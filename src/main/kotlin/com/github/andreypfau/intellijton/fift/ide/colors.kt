package com.github.andreypfau.intellijton.fift.ide

import com.github.andreypfau.intellijton.fift.FiftIcons
import com.github.andreypfau.intellijton.fift.FiftLanguage
import com.github.andreypfau.intellijton.loadTextResource
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import javax.swing.Icon
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors as Defaults

enum class FiftColor(
    displayName: String,
    default: TextAttributesKey
) {
    COMMENT("Comment", Defaults.LINE_COMMENT),

    BRACES("Braces", Defaults.BRACES),
    BRACKETS("Brackets", Defaults.BRACKETS),
    PARENTHESES("Parentheses", Defaults.PARENTHESES),
    SEMICOLON("Semicolon", Defaults.SEMICOLON),

    NUMBER("Number", Defaults.NUMBER),
    STRING("String", Defaults.STRING),
    KEYWORD("Keyword", Defaults.KEYWORD),
    ACTIVE_WORD("Active word", Defaults.FUNCTION_CALL),

    OPERATION_SIGN("Operation signs", Defaults.OPERATION_SIGN),
    CONTRACT_REFERENCE("Contract reference", Defaults.CLASS_REFERENCE)
    ;

    val textAttributesKey =
        TextAttributesKey.createTextAttributesKey("com.github.andreypfau.intellijton.fift.$name", default)
    val attributesDescriptor = AttributesDescriptor(displayName, textAttributesKey)
}


class FiftColorSettingsPage : ColorSettingsPage {
    private val DEMO_TEXT by lazy {
        loadTextResource(this, "colors/highlighter_example.fif")
    }

    override fun getAttributeDescriptors(): Array<AttributesDescriptor> = ATTRIBUTE_DESCRIPTORS
    override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY
    override fun getDisplayName(): String = FiftLanguage.displayName
    override fun getIcon(): Icon = FiftIcons.FILE
    override fun getHighlighter(): SyntaxHighlighter = FiftSyntaxHighlighter
    override fun getDemoText(): String = DEMO_TEXT

    override fun getAdditionalHighlightingTagToDescriptorMap(): MutableMap<String, TextAttributesKey>? = null

    companion object {
        val ATTRIBUTE_DESCRIPTORS = FiftColor.values().map { it.attributesDescriptor }.toTypedArray()
    }
}