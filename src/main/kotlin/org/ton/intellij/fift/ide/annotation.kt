package org.ton.intellij.fift.ide

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement
import org.ton.intellij.fift.psi.FiftWordDefStatement

class FiftAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        when (element) {
            is FiftWordDefStatement -> holder
                .newSilentAnnotation(HighlightSeverity.INFORMATION)
                .range(element.wordDef)
                .textAttributes(FiftColor.WORD_DECLARATION.textAttributesKey)
                .create()
        }
    }
}