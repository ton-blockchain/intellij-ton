package org.ton.intellij.func.converter

import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import org.ton.intellij.func.psi.FuncElementTypes
import org.ton.intellij.func.psi.FuncFile
import org.ton.intellij.func.psi.FuncFunction
import org.ton.intellij.func.psi.FuncHoleType
import org.ton.intellij.func.psi.FuncParenType
import org.ton.intellij.func.psi.FuncPrimitiveType
import org.ton.intellij.func.psi.FuncTensorType
import org.ton.intellij.func.psi.FuncTupleType
import org.ton.intellij.func.psi.FuncTypeReference
import org.ton.intellij.func.psi.impl.isImpure
import org.ton.intellij.func.psi.impl.rawReturnType
import org.ton.intellij.func.type.ty.FuncTyTensor

class FuncToTolkConverterV1 {
    fun convert(file: FuncFile): FuncToTolkState {
        val state = FuncToTolkState(file.text, ConvertFuncToTolkOptions())
        for (child in file.children) {
            convertPsiElement(child, state)
        }
        return state
    }

    fun convertPsiElement(psiElement: PsiElement, out: FuncToTolkState) {
        when (psiElement.elementType) {
            FuncElementTypes.FUNCTION -> {
                (psiElement as? FuncFunction)?.let {
                    convertFunction(it, out)
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
        val typeParameterList = psiElement.typeParameterList.let {
            if (psiElement.forallKeyword != null) it else null
        }
        var returnType = psiElement.typeReference

        val sFunKeyword = out.createEmptyFork(psiElement.firstChild)
        val sName = out.createEmptyFork(nameIdentifier)
        val sGenerics = psiElement.forallKeyword?.let { out.createEmptyFork(it) }
        val sParameters = psiElement.functionParameterList.firstOrNull()?.let { out.createEmptyFork(it) }
        val sReturnType = out.createEmptyFork(returnType)

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

        val localVarNames = extractLocalVarNamesAndParametersFromFunction(psiElement, out)
        out.onEnterFunction(localVarNames)

        if (typeParameterList != null) {
            val joinedT = typeParameterList.joinToString { it.identifier.text }
            sGenerics?.addTextCustom("<$joinedT>")
        }

        if (isModifyingMethod) {
            returnType = extractActualTypeFromModifyingMethod(returnType)
        }
        if (returnType is FuncHoleType && (isModifyingMethod || asmBody != null)) {
            sReturnType.addTextCustom("void")
        } else if (returnType is FuncHoleType) {
            // skip
        } else {
            convertType(returnType, sReturnType)
        }

        val oldName = if (name.firstOrNull() == '~') name.substring(1) else name
        var newName = replaceIdentifier(oldName, out, false)
        FuncRenamingMapping.ENTRYPOINT_RENAMING[newName]?.let {
            newName = it
        } ?: transformSnakeCaseToCamelCase(newName)
        sName.addTextModified(nameIdentifier, newName)


    }

    fun extractActualTypeFromModifyingMethod(
        typeReference: FuncTypeReference,
    ): FuncTypeReference {
        if (typeReference is FuncParenType) {
            return extractActualTypeFromModifyingMethod(typeReference.typeReference ?: return typeReference)
        }

        if (typeReference is FuncTensorType) {
            return typeReference.typeReferenceList.getOrNull(1) ?: typeReference
        }

        return typeReference
    }

    fun extractLocalVarNamesAndParametersFromFunction(
        psiElement: FuncFunction,
        out: FuncToTolkState
    ): MutableSet<String> {
        val varNames = mutableSetOf<String>()
        for (parameter in psiElement.functionParameterList) {
            val name = parameter.name ?: continue
            varNames.add(replaceIdentifier(name, out, false))
        }
        return varNames
    }

    fun convertType(typeReference: FuncTypeReference, out: FuncToTolkState) {
        when (typeReference) {
            is FuncParenType -> {
                convertType(typeReference.typeReference ?: return, out)
            }

            is FuncPrimitiveType -> {
                if (typeReference.contKeyword != null) {
                    out.addTextModified(typeReference, "continuation")
                } else {
                    out.addTextUnchanged(typeReference)
                }
            }

            is FuncTensorType -> {
                out.addTextCustom("(")
                typeReference.typeReferenceList.forEach {
                    convertType(it, out)
                }
                out.addTextCustom(")")
            }
            is FuncTupleType -> {
                out.addTextCustom("[")
                typeReference.typeReferenceList.forEach {
                    convertType(it, out)
                }
                out.addTextCustom("]")
            }
            is FuncHoleType -> {
                out.addTextModified(typeReference, "auto")
            }
            else -> {
                out.addTextUnchanged(typeReference)
            }
        }
    }

    private val VALID_IDENTIFIER_REGEX = Regex("^[a-zA-Z_$][a-zA-Z_$0-9]*$")

    fun replaceIdentifier(name: String, out: FuncToTolkState, isFunctionCall: Boolean): String {
        var newName = name
        if (newName.startsWith("`")) {
            return newName
        }
        if (!isFunctionCall && out.isInsideModifyingMethod && newName == out.selfVarNameInModifyingMethod) {
            return "self"
        }
        if (isFunctionCall && newName in FuncRenamingMapping.STDLIB_RENAMING) {
            newName = FuncRenamingMapping.STDLIB_RENAMING[newName] ?: newName
        }
        if (!isFunctionCall && newName in FuncRenamingMapping.KEYWORDS_RENAMING) {
            newName = FuncRenamingMapping.KEYWORDS_RENAMING[newName] ?: newName
        }
        if (newName.endsWith("?")) {
            newName = if (newName.startsWith("is_")) "" else "is_" + newName.substring(0, newName.length - 1)
        }
        if (!VALID_IDENTIFIER_REGEX.matches(newName)) {
            newName = "`$newName`"
        }
        if (isFunctionCall && FuncRenamingMapping.STDLIB_AUTO_IMPORTS.containsKey(newName)) {
            FuncRenamingMapping.STDLIB_AUTO_IMPORTS[newName]?.let {
                out.autoImportStdlib(it)
            }
        }
        return newName
    }

    fun transformSnakeCaseToCamelCase(name: String): String {
        if (name.startsWith("`")) {
            return name
        }
        val newName = StringBuilder()
        var skip = false
        for (i in name.indices) {
            if (skip) {
                skip = false
                continue
            }
            if (name[i] == '_' && i < name.lastIndex && name[i + 1] >= 'a' && name[i + 1] <= 'z') {
                newName.append(name[i + 1].uppercase())
                skip = true
            } else {
                newName.append(name[i])
            }
        }
        return newName.toString()
    }
}
