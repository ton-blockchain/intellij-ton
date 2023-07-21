package org.ton.intellij.func.highlighting

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.options.colors.AttributesDescriptor

enum class FuncSyntaxHighlightingColors(
    displayName: String,
    default: TextAttributesKey
) {
    LINE_COMMENT("Line comment", DefaultLanguageHighlighterColors.LINE_COMMENT),
    BLOCK_COMMENT("Block comment", DefaultLanguageHighlighterColors.BLOCK_COMMENT),
    DOC_COMMENT("Documentation", DefaultLanguageHighlighterColors.DOC_COMMENT),

    BRACES("Braces", DefaultLanguageHighlighterColors.BRACES),
    BRACKETS("Brackets", DefaultLanguageHighlighterColors.BRACKETS),
    PARENTHESES("Parentheses", DefaultLanguageHighlighterColors.PARENTHESES),
    SEMICOLON("Semicolon", DefaultLanguageHighlighterColors.SEMICOLON),
    COMMA("Comma", DefaultLanguageHighlighterColors.COMMA),
    DOT("Dot", DefaultLanguageHighlighterColors.DOT),
    OPERATION_SIGN("Operation signs", DefaultLanguageHighlighterColors.OPERATION_SIGN),

    NUMBER("Number", DefaultLanguageHighlighterColors.NUMBER),
    STRING("String", DefaultLanguageHighlighterColors.STRING),
    KEYWORD("Keyword", DefaultLanguageHighlighterColors.KEYWORD),
    PRIMITIVE_TYPES("Primitive types", DefaultLanguageHighlighterColors.KEYWORD),
    FUNCTION_DECLARATION("Function declaration", DefaultLanguageHighlighterColors.FUNCTION_DECLARATION),
    FUNCTION_CALL("Function call", DefaultLanguageHighlighterColors.FUNCTION_CALL),
    CONSTANT("Constant", DefaultLanguageHighlighterColors.CONSTANT),
    GLOBAL_VARIABLE("Global variable", DefaultLanguageHighlighterColors.GLOBAL_VARIABLE),
    LOCAL_VARIABLE("Local variable", DefaultLanguageHighlighterColors.LOCAL_VARIABLE),
    MACRO("Macro", DefaultLanguageHighlighterColors.METADATA),
    TYPE_PARAMETER("Type parameter", TextAttributesKey.find("TYPE_PARAMETER_NAME_ATTRIBUTES")),
    ;

    val textAttributesKey =
        TextAttributesKey.createTextAttributesKey("org.ton.intellij.func.$name", default)
    val attributesDescriptor = AttributesDescriptor(displayName, textAttributesKey)

    val attributes
        get() = EditorColorsManager.getInstance().globalScheme.getAttributes(textAttributesKey)
}
