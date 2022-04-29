package org.ton.intellij.func.ide

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement
import org.ton.intellij.func.psi.*

class FuncAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        when (element) {
            is FuncFunctionName -> holder
                .newSilentAnnotation(HighlightSeverity.INFORMATION)
                .range(element)
                .textAttributes(FuncColor.FUNCTION_DECLARATION.textAttributesKey)
                .create()
            is FuncTypeIdentifier -> holder
                .newSilentAnnotation(HighlightSeverity.INFORMATION)
                .range(element)
                .textAttributes(FuncColor.PARAMETER.textAttributesKey)
                .create()
            is FuncGlobalVar -> holder
                .newSilentAnnotation(HighlightSeverity.INFORMATION)
                .range(element.identifier)
                .textAttributes(FuncColor.GLOBAL_VARIABLE.textAttributesKey)
                .create()
            is FuncConstDeclaration -> holder
                .newSilentAnnotation(HighlightSeverity.INFORMATION)
                .range(element.identifier)
                .textAttributes(FuncColor.CONSTANT.textAttributesKey)
                .create()
            is FuncFunctionCallIdentifier -> holder
                .newSilentAnnotation(HighlightSeverity.INFORMATION)
                .range(element)
                .textAttributes(FuncColor.FUNCTION_CALL.textAttributesKey)
                .create()
            is FuncMethodCallIdentifier -> holder
                .newSilentAnnotation(HighlightSeverity.INFORMATION)
                .range(element)
                .textAttributes(FuncColor.FUNCTION_CALL.textAttributesKey)
                .create()
            is FuncModifyingMethodCall -> holder
                .newSilentAnnotation(HighlightSeverity.INFORMATION)
                .range(element)
                .textAttributes(FuncColor.FUNCTION_CALL.textAttributesKey)
                .create()
            is FuncReferenceExpression -> {
                when (element.reference?.resolve()) {
                    is FuncVariableDeclaration -> holder
                        .newSilentAnnotation(HighlightSeverity.INFORMATION)
                        .range(element.identifier)
                        .textAttributes(FuncColor.LOCAL_VARIABLE.textAttributesKey)
                        .create()
                    is FuncConstDeclaration -> holder
                        .newSilentAnnotation(HighlightSeverity.INFORMATION)
                        .range(element.identifier)
                        .textAttributes(FuncColor.CONSTANT.textAttributesKey)
                        .create()
                    is FuncGlobalVar -> holder
                        .newSilentAnnotation(HighlightSeverity.INFORMATION)
                        .range(element.identifier)
                        .textAttributes(FuncColor.GLOBAL_VARIABLE.textAttributesKey)
                        .create()
                }
            }
        }
    }
}