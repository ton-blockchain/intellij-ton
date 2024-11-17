package org.ton.intellij.func.converter

import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import org.ton.intellij.func.psi.FuncElementTypes
import org.ton.intellij.func.psi.FuncFile
import org.ton.intellij.func.psi.FuncFunction
import org.ton.intellij.func.psi.impl.isImpure
import org.ton.intellij.func.psi.impl.rawReturnType
import org.ton.intellij.func.type.ty.FuncTyTensor

class FuncToTolkConverter {
    fun convert(file: FuncFile) {
        file.ch
    }

    fun convertNode(psiElement: PsiElement) {
        when (psiElement.elementType) {
            FuncElementTypes.FUNCTION -> {
                (psiElement as? FuncFunction)?.let {
                    convertFunction(it)
                }
            }
        }
    }

    fun convertFunction(psiElement: FuncFunction, out: FuncToTolkState) {
        val asmBody = psiElement.asmDefinition
        val isImpure = psiElement.isImpure
        val nameIdentifier = psiElement.nameIdentifier ?: return
        val name = psiElement.name ?: return
        val isPure = asmBody != null && !isImpure
        val isInline = psiElement.inlineKeyword != null
        val isInlineRef = psiElement.inlineRefKeyword != null
        val isMethodId = psiElement.methodIdDefinition?.lparen == null
        val methodIdPsi = psiElement.methodIdDefinition

        val sFunKeyword = out.createEmptyFork(psiElement.firstChild)
        val sName = out.createEmptyFork(nameIdentifier)

        if (methodIdPsi != null) {
            out.registerKnownGetMethod(name)
        }

        if (psiElement.blockStatement == null && psiElement.asmDefinition == null) {
            out.justSkipPsi(psiElement)
            return
        }

        var isModifyingMethod = name.firstOrNull() == '~'
        if (name.startsWith("load_") || name.startsWith("skip_") || name.startsWith("store_") || name.startsWith("set_")) {
            val retType = psiElement.rawReturnType
            if (retType is FuncTyTensor && retType.types.size == 2 && psiElement.functionParameterList.size > 2) {
                isModifyingMethod = true
            }
        }


    }

    fun extractLocalVarNamesAndParametersFromFunction(
        psiElement: FuncFunction,
        out: FuncToTolkState
    ) {
        val varNames = mutableSetOf<String>()
        for (parameter in psiElement.functionParameterList) {

        }
    }

    fun replaceIdentifier(name: String, out: FuncToTolkState, isFunctionCall: Boolean): String {
        if (name.startsWith("`")) {
            return name
        }
        if (!isFunctionCall && out.isInsideModifyingMethod && name == out.selfVarNameInModifyingMethod) {
            return "self"
        }
        if (isFunctionCall && name in )
    }
}
