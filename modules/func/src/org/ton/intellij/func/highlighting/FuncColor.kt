package org.ton.intellij.func.highlighting

import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors as Default

enum class FuncColor(
    displayName: String,
    default: TextAttributesKey,
) {
    LINE_COMMENT("Comments//Line comment", Default.LINE_COMMENT),
    BLOCK_COMMENT("Comments//Block comment", Default.BLOCK_COMMENT),
    DOC_COMMENT("Documentation//Comment", Default.DOC_COMMENT),
    DOC_CODE("Documentation//Code", Default.DOC_COMMENT_MARKUP),
    DOC_LINK("Documentation//Link", Default.DOC_COMMENT_TAG_VALUE),

    BRACES("Braces and Operators//Braces", Default.BRACES),
    BRACKETS("Braces and Operators//Brackets", Default.BRACKETS),
    PARENTHESES("Braces and Operators//Parentheses", Default.PARENTHESES),
    SEMICOLON("Braces and Operators//Semicolon", Default.SEMICOLON),
    COMMA("Braces and Operators//Comma", Default.COMMA),
    OPERATION_SIGN("Braces and Operators//Operation signs", Default.OPERATION_SIGN),

    NUMBER("Literals//Number", Default.NUMBER),
    STRING("Literals//String", Default.STRING),

    KEYWORD("Keyword", Default.KEYWORD),

    PRIMITIVE_TYPES("Types//Primitive types", Default.KEYWORD),

    FUNCTION_DECLARATION("Functions//Function declaration", Default.FUNCTION_DECLARATION),
    FUNCTION_CALL("Functions//Function call", Default.FUNCTION_CALL),

    CONSTANT("Variables//Constant", Default.CONSTANT),
    GLOBAL_VARIABLE("Variables//Global variable", Default.GLOBAL_VARIABLE),
    LOCAL_VARIABLE("Variables//Local variable", Default.LOCAL_VARIABLE),

    MACRO("Macro", Default.METADATA),

    PARAMETER("Parameters//Parameter", Default.PARAMETER),
    TYPE_PARAMETER("Parameters//Type parameter", TextAttributesKey.find("TYPE_PARAMETER_NAME_ATTRIBUTES")),
    ;

    val textAttributesKey =
        TextAttributesKey.createTextAttributesKey("org.ton.intellij.func.$name", default)
    val attributesDescriptor = AttributesDescriptor(displayName, textAttributesKey)

    val attributes
        get() = EditorColorsManager.getInstance().globalScheme.getAttributes(textAttributesKey)
}
