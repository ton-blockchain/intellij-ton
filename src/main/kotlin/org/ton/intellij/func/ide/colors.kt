package org.ton.intellij.func.ide

import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import org.ton.intellij.func.FuncIcons
import org.ton.intellij.func.FuncLanguage
import org.ton.intellij.loadTextResource
import javax.swing.Icon
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors as Defaults

enum class FuncColor(
    displayName: String,
    default: TextAttributesKey
) {
    COMMENT("Comment", Defaults.LINE_COMMENT),
    DOCUMENTATION("Documentation", Defaults.DOC_COMMENT),

    BRACES("Braces", Defaults.BRACES),
    BRACKETS("Brackets", Defaults.BRACKETS),
    PARENTHESES("Parentheses", Defaults.PARENTHESES),
    SEMICOLON("Semicolon", Defaults.SEMICOLON),
    COMMA("Comma", Defaults.COMMA),
    DOT("Dot", Defaults.DOT),
    OPERATION_SIGN("Operation signs", Defaults.OPERATION_SIGN),

    NUMBER("Number", Defaults.NUMBER),
    STRING("String", Defaults.STRING),
    KEYWORD("Keyword", Defaults.KEYWORD),
    PRIMITIVE_TYPES("Primitive types", Defaults.KEYWORD),
    FUNCTION_DECLARATION("Function declaration", Defaults.FUNCTION_DECLARATION),
    FUNCTION_CALL("Function call", Defaults.FUNCTION_CALL),
    PARAMETER("Type parameter", TextAttributesKey.find("KOTLIN_TYPE_PARAMETER")),
    CONSTANT("Constant", Defaults.CONSTANT),
    GLOBAL_VARIABLE("Global variable", Defaults.GLOBAL_VARIABLE),
    LOCAL_VARIABLE("Local variable", Defaults.LOCAL_VARIABLE),
    MACRO("Macro", Defaults.METADATA)
    ;

    val textAttributesKey =
        TextAttributesKey.createTextAttributesKey("org.ton.intellij.func.$name", default)
    val attributesDescriptor = AttributesDescriptor(displayName, textAttributesKey)
}


class FuncColorSettingsPage : ColorSettingsPage {
    private val DEMO_TEXT by lazy {
        loadTextResource(FuncColorSettingsPage::class.java, "colors/highlighter_example.fc")
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