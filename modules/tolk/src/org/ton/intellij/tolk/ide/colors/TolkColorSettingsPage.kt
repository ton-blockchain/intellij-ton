package org.ton.intellij.tolk.ide.colors

import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.util.io.StreamUtil
import org.jetbrains.annotations.NonNls
import org.ton.intellij.tolk.TolkIcons
import org.ton.intellij.tolk.highlighting.TolkSyntaxHighlighter
import javax.swing.Icon

class TolkColorSettingsPage : ColorSettingsPage {
    override fun getIcon(): Icon = TolkIcons.FILE

    override fun getHighlighter(): SyntaxHighlighter = TolkSyntaxHighlighter()

    override fun getDemoText(): @NonNls String = DEMO_TEXT

    override fun getAdditionalHighlightingTagToDescriptorMap(): Map<String, TextAttributesKey> = ANNOTATOR_TAGS

    override fun getAttributeDescriptors(): Array<out AttributesDescriptor> = ATTRS

    override fun getColorDescriptors(): Array<out ColorDescriptor?> = ColorDescriptor.EMPTY_ARRAY

    override fun getDisplayName(): @NlsContexts.ConfigurableName String = "Tolk"

    companion object {
        private val ATTRS: Array<AttributesDescriptor> = TolkColor.entries.map { it.attributesDescriptor }.toTypedArray()
        private val ANNOTATOR_TAGS = TolkColor.entries.associateBy({ it.name }, { it.textAttributesKey })

        private val DEMO_TEXT: String by lazy {
            val stream = TolkColorSettingsPage::class.java.classLoader
                .getResourceAsStream("colorSchemes/TolkHighlighterDemo.tolk")
                ?: error("Cannot load resource `colorSchemes/TolkHighlighterDemo.tolk`")
            stream.use {
                StreamUtil.convertSeparators(it.reader().readText())
            }
        }
    }
}
