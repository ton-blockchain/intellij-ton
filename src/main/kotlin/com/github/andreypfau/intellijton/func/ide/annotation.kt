package com.github.andreypfau.intellijton.func.ide

import com.github.andreypfau.intellijton.func.psi.FuncFunctionCallElement
import com.github.andreypfau.intellijton.func.psi.FuncFunctionIdentifier
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement

class FuncAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        when (element) {
            is FuncFunctionIdentifier -> holder
                .newSilentAnnotation(HighlightSeverity.INFORMATION)
                .range(element)
                .textAttributes(FuncColor.FUNCTION_DECLARATION.textAttributesKey)
                .create()
            is FuncFunctionCallElement -> holder
                .newSilentAnnotation(HighlightSeverity.INFORMATION)
                .range(element.referenceNameElement)
                .textAttributes(FuncColor.FUNCTION_CALL.textAttributesKey)
                .create()
        }
    }
}