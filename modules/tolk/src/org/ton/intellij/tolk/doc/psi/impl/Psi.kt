package org.ton.intellij.tolk.doc.psi.impl

import com.intellij.psi.LiteralTextEscaper
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.impl.source.tree.AstBufferUtil
import com.intellij.psi.impl.source.tree.CompositePsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.childrenOfType
import org.ton.intellij.tolk.doc.psi.*
import org.ton.intellij.util.SimpleMultiLineTextEscaper
import org.ton.intellij.util.childOfType

abstract class TolkDocElementImpl(type: IElementType) : CompositePsiElement(type), TolkDocElement {
    protected open fun <T : Any> notNullChild(child: T?): T =
        child ?: error("$text parent=${parent.text}")

    override val containingDoc: TolkDocComment
        get() = PsiTreeUtil.getParentOfType(this, TolkDocComment::class.java, true)
            ?: error("TolkDocElement cannot leave outside of the doc comment! `${text}`")

    override val markdownValue: String
        get() = AstBufferUtil.getTextSkippingWhitespaceComments(this)


    override fun toString(): String = "${javaClass.simpleName}($elementType)"
}

class TolkDocGapImpl(type: IElementType, text: CharSequence) : LeafPsiElement(type, text), TolkDocGap {
    override fun getTokenType(): IElementType = elementType
}

class TolkDocInlineLinkImpl(type: IElementType) : TolkDocElementImpl(type), TolkDocInlineLink {
    override val linkText: TolkDocLinkText
        get() = notNullChild(childOfType())

    override val linkDestination: TolkDocLinkDestination
        get() = notNullChild(childOfType())
}

class TolkDocLinkReferenceShortImpl(type: IElementType) : TolkDocElementImpl(type), TolkDocLinkReferenceShort {
    override val linkLabel: TolkDocLinkLabel
        get() = notNullChild(childOfType())
}

class TolkDocLinkReferenceFullImpl(type: IElementType) : TolkDocElementImpl(type), TolkDocLinkReferenceFull {
    override val linkText: TolkDocLinkText
        get() = notNullChild(childOfType())

    override val linkLabel: TolkDocLinkLabel
        get() = notNullChild(childOfType())
}

class TolkDocLinkDefinitionImpl(type: IElementType) : TolkDocElementImpl(type), TolkDocLinkDefinition {
    override val linkLabel: TolkDocLinkLabel
        get() = notNullChild(childOfType())

    override val linkDestination: TolkDocLinkDestination
        get() = notNullChild(childOfType())
}


class TolkDocLinkTextImpl(type: IElementType) : TolkDocElementImpl(type), TolkDocLinkText
class TolkDocLinkLabelImpl(type: IElementType) : TolkDocElementImpl(type), TolkDocLinkLabel
class TolkDocLinkTitleImpl(type: IElementType) : TolkDocElementImpl(type), TolkDocLinkTitle
class TolkDocLinkDestinationImpl(type: IElementType) : TolkDocElementImpl(type), TolkDocLinkDestination


class TolkDocCodeSpanImpl(type: IElementType) : TolkDocElementImpl(type), TolkDocCodeSpan
class TolkDocCodeBlockImpl(type: IElementType) : TolkDocElementImpl(type), TolkDocCodeBlock
class TolkDocCodeFenceStartEndImpl(type: IElementType) : TolkDocElementImpl(type), TolkDocCodeFenceStartEnd
class TolkDocCodeFenceLangImpl(type: IElementType) : TolkDocElementImpl(type), TolkDocCodeFenceLang

class TolkDocCodeFenceImpl(type: IElementType) : TolkDocElementImpl(type), TolkDocCodeFence {

    override val start: TolkDocCodeFenceStartEnd
        get() = notNullChild(childOfType())
    override val lang: TolkDocCodeFenceLang?
        get() = childOfType()
    override val end: TolkDocCodeFenceStartEnd?
        get() = childrenOfType<TolkDocCodeFenceStartEnd>().getOrNull(1)

    override fun isValidHost(): Boolean = true

    override fun createLiteralTextEscaper(): LiteralTextEscaper<out PsiLanguageInjectionHost> {
        return SimpleMultiLineTextEscaper(this)
    }

    override fun updateText(text: String): PsiLanguageInjectionHost {
        return this
    }
}
