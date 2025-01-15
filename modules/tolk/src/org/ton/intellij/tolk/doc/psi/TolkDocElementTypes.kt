package org.ton.intellij.tolk.doc.psi

import org.ton.intellij.tolk.doc.psi.impl.*

object TolkDocElementTypes {
    val DOC_GAP = TolkDocTokenType("<DOC_GAP>")
    val DOC_TEXT = TolkDocTokenType("<DOC_TEXT>")

    val DOC_CODE_SPAN = TolkDocCompositeTokenType("<DOC_CODE_SPAN>", ::TolkDocCodeSpanImpl)
    val DOC_CODE_FENCE = TolkDocCompositeTokenType("<DOC_CODE_FENCE>", ::TolkDocCodeFenceImpl)
    val DOC_CODE_BLOCK = TolkDocCompositeTokenType("<DOC_CODE_BLOCK>", ::TolkDocCodeBlockImpl)

    val DOC_INLINE_LINK = TolkDocCompositeTokenType("<DOC_INLINE_LINK>", ::TolkDocInlineLinkImpl)
    val DOC_SHORT_REFERENCE_LINK =
        TolkDocCompositeTokenType("<DOC_SHORT_REFERENCE_LINK>", ::TolkDocLinkReferenceShortImpl)
    val DOC_FULL_REFERENCE_LINK = TolkDocCompositeTokenType("<DOC_FULL_REFERENCE_LINK>", ::TolkDocLinkReferenceFullImpl)
    val DOC_LINK_DEFINITION = TolkDocCompositeTokenType("<DOC_LINK_DEFINITION>", ::TolkDocLinkDefinitionImpl)

    val DOC_LINK_TEXT = TolkDocCompositeTokenType("<DOC_LINK_TEXT>", ::TolkDocLinkTextImpl)
    val DOC_LINK_LABEL = TolkDocCompositeTokenType("<DOC_LINK_LABEL>", ::TolkDocLinkLabelImpl)
    val DOC_LINK_TITLE = TolkDocCompositeTokenType("<DOC_LINK_TITLE>", ::TolkDocLinkTitleImpl)
    val DOC_LINK_DESTINATION = TolkDocCompositeTokenType("<DOC_LINK_DESTINATION>", ::TolkDocLinkDestinationImpl)

    val DOC_CODE_FENCE_START_END =
        TolkDocCompositeTokenType("<DOC_CODE_FENCE_START_END>", ::TolkDocCodeFenceStartEndImpl)
    val DOC_CODE_FENCE_LANG = TolkDocCompositeTokenType("<DOC_CODE_FENCE_LANG>", ::TolkDocCodeFenceLangImpl)
}
