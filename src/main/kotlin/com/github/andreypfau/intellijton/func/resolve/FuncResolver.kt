package com.github.andreypfau.intellijton.func.resolve

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.findParentOfType

object FuncResolver {
    fun PsiElement.resolveFile(): PsiFile = if (this is PsiFile) this else findParentOfType()!!
//
//    fun resolveFunctions(element: PsiElement) =
//        element.resolveFile().childrenOfType<FuncFunctionDefinition>().asSequence()
//
//    fun resolveVarLiteralReference(element: FuncNamedElement): Sequence<FuncNamedElement> {
//        val functionCall = element.parent?.parent
//        return if (functionCall is FuncFunctionCallMixin) {
//            return resolveFunctions(functionCall).filter { it.name == element.name }
//        } else {
//            resolveVarLiteral(element)
//        }
//    }
//
//    fun resolveVarLiteral(element: FuncNamedElement): Sequence<FuncNamedElement> {
//        return emptySequence()
//    }
}