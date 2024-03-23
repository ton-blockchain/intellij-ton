package org.ton.intellij.tact.highlighting

import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors as Default

enum class TactColor(
    displayName: String,
    default: TextAttributesKey,
) {
    LINE_COMMENT("Line comment", Default.LINE_COMMENT),
    BLOCK_COMMENT("Block comment", Default.BLOCK_COMMENT),
    DOC_COMMENT("Documentation", Default.DOC_COMMENT),
    DOC_CODE("Documentation Code", Default.DOC_COMMENT_MARKUP),
    DOC_LINK("Documentation Link", Default.DOC_COMMENT_TAG_VALUE),

    BRACES("Braces", Default.BRACES),
    BRACKETS("Brackets", Default.BRACKETS),
    PARENTHESES("Parentheses", Default.PARENTHESES),
    SEMICOLON("Semicolon", Default.SEMICOLON),
    COMMA("Comma", Default.COMMA),
    DOT("Dot", Default.DOT),
    OPERATION_SIGN("Operation signs", Default.OPERATION_SIGN),

    NUMBER("Number", Default.NUMBER),
    STRING("String", Default.STRING),
    KEYWORD("Keyword", Default.KEYWORD),
    PRIMITIVE_TYPES("Primitive types", Default.KEYWORD),
    FUNCTION_DECLARATION("Function declaration", Default.FUNCTION_DECLARATION),
    FUNCTION_CALL("Function call", Default.FUNCTION_CALL),
    FUNCTION_STATIC("Function static", Default.STATIC_METHOD),
    FIELD("Variables//Field", Default.INSTANCE_FIELD),
    CONSTANT("Variables//Constant", Default.CONSTANT),
    SELF_PARAMETER("Parameters//Self parameter", Default.KEYWORD),
    GLOBAL_VARIABLE("Global variable", Default.GLOBAL_VARIABLE),
    LOCAL_VARIABLE("Local variable", Default.LOCAL_VARIABLE),
    MACRO("Macro", Default.METADATA),
    PARAMETER("Parameter", Default.PARAMETER),
    IDENTIFIER("Identifier", Default.IDENTIFIER),
    TYPE_PARAMETER("Type parameter", TextAttributesKey.find("TYPE_PARAMETER_NAME_ATTRIBUTES")),
    ;

    val textAttributesKey =
        TextAttributesKey.createTextAttributesKey("org.ton.intellij.tact.$name", default)
    val attributesDescriptor = AttributesDescriptor(displayName, textAttributesKey)

    val attributes
        get() = EditorColorsManager.getInstance().globalScheme.getAttributes(textAttributesKey)
}
