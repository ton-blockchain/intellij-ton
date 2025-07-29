package org.ton.intellij.fift.ide

import com.intellij.openapi.editor.XmlHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import org.ton.intellij.fift.FiftIcons
import org.ton.intellij.fift.FiftLanguage
import org.ton.intellij.util.loadTextResource
import javax.swing.Icon
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors as Defaults

enum class FiftColor(
    displayName: String,
    default: TextAttributesKey,
) {
    COMMENT("Comment", Defaults.LINE_COMMENT),
    DOCUMENTATION("Documentation", Defaults.DOC_COMMENT),

    BRACES("Braces", Defaults.BRACES),
    BRACKETS("Brackets", Defaults.BRACKETS),
    PARENTHESES("Parentheses", Defaults.PARENTHESES),
    SEMICOLON("Semicolon", Defaults.SEMICOLON),

    NUMBER("Number", Defaults.NUMBER),
    STRING("String", Defaults.STRING),
    KEYWORD("Keyword", Defaults.KEYWORD),

    OPERATION_SIGN("Operation signs", Defaults.OPERATION_SIGN),
    WORD_DECLARATION("Word declaration", Defaults.FUNCTION_DECLARATION),
    STRING_WORD("String parameter word", Defaults.INSTANCE_FIELD),

    ASSEMBLY_DEFINITION("Assembly definition", Defaults.FUNCTION_DECLARATION),
    ASSEMBLY_CALL("Assembly call", Defaults.FUNCTION_DECLARATION),
    ASSEMBLY_INSTRUCTION("Assembly instruction", XmlHighlighterColors.HTML_TAG)
    ;

    val textAttributesKey =
        TextAttributesKey.createTextAttributesKey("org.ton.intellij.fift.$name", default)
    val attributesDescriptor = AttributesDescriptor(displayName, textAttributesKey)
}


class FiftColorSettingsPage : ColorSettingsPage {
    private val DEMO_TEXT by lazy {
        loadTextResource(FiftColorSettingsPage::class.java, "colors/highlighter_example.fif")
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
