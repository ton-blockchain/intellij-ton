package org.ton.intellij.tolk.highlighting

import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.options.colors.AttributesDescriptor
import org.ton.intellij.tolk.TolkBundle
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors as Default

enum class TolkColor(
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

    PARAMETER(TolkBundle.message("settings.tolk.color.parameter"), Default.PARAMETER),
    MUT_PARAMETER(TolkBundle.message("settings.tolk.color.mutable_parameter"), Default.PARAMETER),
    SELF_PARAMETER(TolkBundle.message("settings.tolk.color.self_parameter"), Default.KEYWORD),
    TYPE_PARAMETER(TolkBundle.message("settings.tolk.color.type_parameter"), TextAttributesKey.find("TYPE_PARAMETER_NAME_ATTRIBUTES")),

    PRIMITIVE(TolkBundle.message("settings.tolk.color.primitive"), Default.KEYWORD),

    NUMBER("Number", Default.NUMBER),
    STRING("String", Default.STRING),
    KEYWORD("Keyword", Default.KEYWORD),
    FUNCTION_DECLARATION("Function declaration", Default.FUNCTION_DECLARATION),
    FUNCTION_CALL("Function call", Default.FUNCTION_CALL),
    FUNCTION_STATIC("Function static", Default.STATIC_METHOD),
    CONSTANT("Constant", Default.CONSTANT),
    GLOBAL_VARIABLE("Global variable", Default.GLOBAL_VARIABLE),
    LOCAL_VARIABLE("Local variable", Default.LOCAL_VARIABLE),
    MACRO("Macro", Default.METADATA),
    ANNOTATION("Annotation", Default.METADATA),
    IDENTIFIER("Identifier", Default.IDENTIFIER),
    ;

    val textAttributesKey =
        TextAttributesKey.createTextAttributesKey("org.ton.intellij.tolk.$name", default)
    val attributesDescriptor = AttributesDescriptor(displayName, textAttributesKey)

    val attributes
        get() = EditorColorsManager.getInstance().globalScheme.getAttributes(textAttributesKey)
}
