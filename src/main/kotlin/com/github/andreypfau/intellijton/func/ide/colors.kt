package com.github.andreypfau.intellijton.func.ide

import com.github.andreypfau.intellijton.func.FuncIcons
import com.github.andreypfau.intellijton.func.FuncLanguage
import com.github.andreypfau.intellijton.loadTextResource
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import javax.swing.Icon
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors as Defaults

enum class FuncColor(
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
    FUNCTION_DECLARATION("Function declaration", Defaults.FUNCTION_DECLARATION),
    FUNCTION_CALL("Function call", Defaults.FUNCTION_CALL),
    PARAMETER("Type parameter", TextAttributesKey.find("KOTLIN_TYPE_PARAMETER")),

    OPERATION_SIGN("Operation signs", Defaults.OPERATION_SIGN),
    ;

    val textAttributesKey =
        TextAttributesKey.createTextAttributesKey("com.github.andreypfau.intellijton.func.$name", default)
    val attributesDescriptor = AttributesDescriptor(displayName, textAttributesKey)
}


class FuncColorSettingsPage : ColorSettingsPage {
    private val DEMO_TEXT by lazy {
        loadTextResource(this, "colors/highlighter_example.fc")
    }

    override fun getAttributeDescriptors(): Array<AttributesDescriptor> = ATTRIBUTE_DESCRIPTORS
    override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY
    override fun getDisplayName(): String = FuncLanguage.displayName
    override fun getIcon(): Icon = FuncIcons.FILE
    override fun getHighlighter(): SyntaxHighlighter = FuncSyntaxHighlighter
    override fun getDemoText(): String = DEMO_TEXT

    override fun getAdditionalHighlightingTagToDescriptorMap(): MutableMap<String, TextAttributesKey>? = null

    companion object {
        val ATTRIBUTE_DESCRIPTORS = FuncColor.values().map { it.attributesDescriptor }.toTypedArray()
    }
}