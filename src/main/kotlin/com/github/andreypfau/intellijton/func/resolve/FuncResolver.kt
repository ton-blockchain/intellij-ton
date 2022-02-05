package com.github.andreypfau.intellijton.func.resolve

import com.github.andreypfau.intellijton.func.psi.FuncElement
import com.github.andreypfau.intellijton.func.psi.FuncFunctionCallMixin
import com.github.andreypfau.intellijton.func.psi.FuncFunctionDefinition
import com.github.andreypfau.intellijton.func.psi.FuncNamedElement
import com.github.andreypfau.intellijton.psiFile
import com.intellij.psi.PsiFile
import com.intellij.psi.util.childrenOfType
import com.intellij.psi.util.findParentOfType

object FuncResolver {
    fun FuncElement.resolveFile(): PsiFile = findParentOfType()!!
    fun resolveFunctions(element: FuncElement) =
        element.psiFile().childrenOfType<FuncFunctionDefinition>().asSequence()

    fun resolveVarLiteralReference(element: FuncNamedElement): Sequence<FuncNamedElement> {
        val functionCall = element.parent?.parent
        return if (functionCall is FuncFunctionCallMixin) {
            val resolved = functionCall.reference?.multiResolve()?.toList() ?: emptyList()
            return if (resolved.isNotEmpty()) {
                resolved.asSequence().filterIsInstance<FuncNamedElement>()
            } else {
                resolveVarLiteral(element)
            }
        } else {
            resolveVarLiteral(element)
        }
    }

    fun resolveVarLiteral(element: FuncNamedElement): Sequence<FuncNamedElement> {
        return emptySequence()
    }
}