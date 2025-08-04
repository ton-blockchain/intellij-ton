package org.ton.intellij.tolk.ide.colors

import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.options.colors.AttributesDescriptor
import org.ton.intellij.tolk.TolkBundle
import java.util.function.Supplier
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors as Default


enum class TolkColor(
    displayName: Supplier<String>,
    default: TextAttributesKey,
) {
    VARIABLE(TolkBundle.messagePointer("settings.tolk.color.variable"), Default.LOCAL_VARIABLE),
    MUTABLE_VARIABLE(TolkBundle.messagePointer("settings.tolk.color.variable.mutable"), Default.REASSIGNED_LOCAL_VARIABLE),
    FIELD(TolkBundle.messagePointer("settings.tolk.color.field"), Default.INSTANCE_FIELD),
    CONSTANT(TolkBundle.messagePointer("settings.tolk.color.constant"), Default.CONSTANT),
    GLOBAL_VARIABLE(TolkBundle.messagePointer("settings.tolk.color.global"), Default.GLOBAL_VARIABLE),

    FUNCTION(TolkBundle.messagePointer("settings.tolk.color.function.declaration"), Default.FUNCTION_DECLARATION),
    METHOD(TolkBundle.messagePointer("settings.tolk.color.method.declaration"), Default.INSTANCE_METHOD),
    STATIC_FUNCTION(TolkBundle.messagePointer("settings.tolk.color.static_function.declaration"), Default.STATIC_METHOD),
    FUNCTION_CALL(TolkBundle.messagePointer("settings.tolk.color.function.call"), Default.FUNCTION_DECLARATION),
    METHOD_CALL(TolkBundle.messagePointer("settings.tolk.color.method.call"), Default.INSTANCE_METHOD),
    STATIC_FUNCTION_CALL(TolkBundle.messagePointer("settings.tolk.color.static_function.call"), Default.STATIC_METHOD),

    PARAMETER(TolkBundle.messagePointer("settings.tolk.color.parameter"), Default.PARAMETER),
    MUT_PARAMETER(TolkBundle.messagePointer("settings.tolk.color.mutable_parameter"), Default.PARAMETER),
    SELF_PARAMETER(TolkBundle.messagePointer("settings.tolk.color.self_parameter"), Default.KEYWORD),
    MUT_SELF_PARAMETER(TolkBundle.messagePointer("settings.tolk.color.mutable_self_parameter"), Default.KEYWORD),
    TYPE_PARAMETER(
        TolkBundle.messagePointer("settings.tolk.color.type_parameter"),
        TextAttributesKey.find("TYPE_PARAMETER_NAME_ATTRIBUTES")
    ),

    PRIMITIVE(TolkBundle.messagePointer("settings.tolk.color.primitive"), Default.KEYWORD),
    STRUCT(TolkBundle.messagePointer("settings.tolk.color.struct"), Default.CLASS_NAME),
    EMPTY_STRUCT(TolkBundle.messagePointer("settings.tolk.color.empty_struct"), Default.CLASS_NAME),
    TYPE_ALIAS(TolkBundle.messagePointer("settings.tolk.color.type_alias"), Default.INTERFACE_NAME),

    KEYWORD(TolkBundle.messagePointer("settings.tolk.color.keyword"), Default.KEYWORD),
    NUMBER(TolkBundle.messagePointer("settings.tolk.color.number"), Default.NUMBER),
    STRING(TolkBundle.messagePointer("settings.tolk.color.string"), Default.STRING),
    
    LINE_COMMENT(TolkBundle.messagePointer("settings.tolk.color.line_comment"), Default.LINE_COMMENT),
    BLOCK_COMMENT(TolkBundle.messagePointer("settings.tolk.color.block_comment"), Default.BLOCK_COMMENT),
    DOC_COMMENT(TolkBundle.messagePointer("settings.tolk.color.doc.comment"), Default.DOC_COMMENT),
    DOC_CODE(TolkBundle.messagePointer("settings.tolk.color.doc.code"), Default.DOC_COMMENT_MARKUP),
    DOC_LINK(TolkBundle.messagePointer("settings.tolk.color.doc.link"), Default.DOC_COMMENT_TAG_VALUE),

    BRACES(TolkBundle.messagePointer("settings.tolk.color.braces"), Default.BRACES),
    BRACKETS(TolkBundle.messagePointer("settings.tolk.color.brackets"), Default.BRACKETS),
    PARENTHESES(TolkBundle.messagePointer("settings.tolk.color.parentheses"), Default.PARENTHESES),
    SEMICOLON(TolkBundle.messagePointer("settings.tolk.color.semicolon"), Default.SEMICOLON),
    COMMA(TolkBundle.messagePointer("settings.tolk.color.comma"), Default.COMMA),
    DOT(TolkBundle.messagePointer("settings.tolk.color.dot"), Default.DOT),
    OPERATION_SIGN(TolkBundle.messagePointer("settings.tolk.color.operation_sign"), Default.OPERATION_SIGN),
    
    ANNOTATION(TolkBundle.messagePointer("settings.tolk.color.annotation"), Default.METADATA),
    ;

    val textAttributesKey =
        TextAttributesKey.createTextAttributesKey("org.ton.tolk.$name", default)
    val attributesDescriptor = AttributesDescriptor(displayName, textAttributesKey)

    val attributes
        get() = EditorColorsManager.getInstance().globalScheme.getAttributes(textAttributesKey)
}
