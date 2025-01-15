package org.ton.intellij.tact

import com.intellij.codeInsight.editorActions.SimpleTokenSetQuoteHandler
import org.ton.intellij.tact.psi.TactElementTypes

class TactQuoteHandler : SimpleTokenSetQuoteHandler(TactElementTypes.STRING_LITERAL)
