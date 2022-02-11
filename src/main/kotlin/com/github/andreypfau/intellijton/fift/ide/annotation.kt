package com.github.andreypfau.intellijton.fift.ide

import com.github.andreypfau.intellijton.fift.psi.FiftWordDef
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement

class FiftAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        when (element) {
            is FiftWordDef -> holder
                .newSilentAnnotation(HighlightSeverity.INFORMATION)
                .range(element.word)
                .textAttributes(FiftColor.WORD_DECLARATION.textAttributesKey)
                .create()
        }
    }
}