package org.ton.intellij.func.ide.colors

import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.util.io.StreamUtil
import org.jetbrains.annotations.NonNls
import org.ton.intellij.func.FuncIcons
import org.ton.intellij.func.highlighting.FuncColor
import org.ton.intellij.func.highlighting.FuncSyntaxHighlighter
import javax.swing.Icon

class FuncColorSettingsPage : ColorSettingsPage {
    override fun getIcon(): Icon = FuncIcons.FILE

    override fun getHighlighter(): SyntaxHighlighter = FuncSyntaxHighlighter()

    override fun getDemoText(): @NonNls String = DEMO_TEXT

    override fun getAdditionalHighlightingTagToDescriptorMap(): Map<String, TextAttributesKey> = ANNOTATOR_TAGS

    override fun getAttributeDescriptors(): Array<out AttributesDescriptor> = ATTRS

    override fun getColorDescriptors(): Array<out ColorDescriptor?> = ColorDescriptor.EMPTY_ARRAY

    override fun getDisplayName(): @NlsContexts.ConfigurableName String = "FunC"

    companion object {
        private val ATTRS: Array<AttributesDescriptor> = FuncColor.entries.map { it.attributesDescriptor }.toTypedArray()
        private val ANNOTATOR_TAGS = FuncColor.entries.associateBy({ it.name }, { it.textAttributesKey })

        private val DEMO_TEXT: String by lazy {
            val stream = FuncColorSettingsPage::class.java.classLoader
                .getResourceAsStream("colorSchemes/FuncHighlighterDemo.fc")
                ?: error("Cannot load resource `colorSchemes/FuncHighlighterDemo.fc`")
            stream.use {
                StreamUtil.convertSeparators(it.reader().readText())
            }
        }
    }
}
