package org.ton.intellij.func.doc.psi

import org.ton.intellij.func.doc.psi.impl.*

object FuncDocElementTypes {
    val DOC_GAP = FuncDocTokenType("<DOC_GAP>")
    val DOC_TEXT = FuncDocTokenType("<DOC_TEXT>")

    val DOC_CODE_SPAN = FuncDocCompositeTokenType("<DOC_CODE_SPAN>", ::FuncDocCodeSpanImpl)

    val DOC_INLINE_LINK = FuncDocCompositeTokenType("<DOC_INLINE_LINK>", ::FuncDocInlineLinkImpl)
    val DOC_SHORT_REFERENCE_LINK =
        FuncDocCompositeTokenType("<DOC_SHORT_REFERENCE_LINK>", ::FuncDocLinkReferenceShortImpl)
    val DOC_FULL_REFERENCE_LINK = FuncDocCompositeTokenType("<DOC_FULL_REFERENCE_LINK>", ::FuncDocLinkReferenceFullImpl)
    val DOC_LINK_DEFINITION = FuncDocCompositeTokenType("<DOC_LINK_DEFINITION>", ::FuncDocLinkDefinitionImpl)

    val DOC_LINK_TEXT = FuncDocCompositeTokenType("<DOC_LINK_TEXT>", ::FuncDocLinkTextImpl)
    val DOC_LINK_LABEL = FuncDocCompositeTokenType("<DOC_LINK_LABEL>", ::FuncDocLinkLabelImpl)
    val DOC_LINK_TITLE = FuncDocCompositeTokenType("<DOC_LINK_TITLE>", ::FuncDocLinkTitleImpl)
    val DOC_LINK_DESTINATION = FuncDocCompositeTokenType("<DOC_LINK_DESTINATION>", ::FuncDocLinkDestinationImpl)
}
