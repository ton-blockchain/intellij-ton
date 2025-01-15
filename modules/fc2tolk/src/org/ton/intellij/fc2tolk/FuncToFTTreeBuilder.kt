package org.ton.intellij.fc2tolk

import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import org.ton.intellij.fc2tolk.tree.*
import org.ton.intellij.func.psi.*

class FuncToFTTreeBuilder {
    private val expressionTreeMapper = ExpressionTreeMapper()
    private val declarationMapper = DeclarationMapper(expressionTreeMapper)

    fun buildTree(psi: PsiElement): FTTreeRoot? {
        return when (psi) {
            is FuncFile -> psi.toFT()
            is FuncFunction -> with(declarationMapper) { psi.toFt() }

            else -> null
        }?.let {
            FTTreeRoot(it)
        }
    }

    private fun FuncFile.toFT(): FTFile {
        return FTFile(
            with(declarationMapper) {
                functions.map { it.toFt() }
            },
        )
    }

    private fun FuncTypeReference?.toFt(): FTType = if (this == null) FTUnknownType else when (this) {
        is FuncPrimitiveType -> this.toFt()
        is FuncTensorType -> FTTensorType.create(typeReferenceList.map { it.toFt() })
        is FuncTupleType -> FTTupleType(typeReferenceList.map { it.toFt() })
        is FuncParenType -> typeReference.toFt()
        is FuncHoleType -> FTFuncHoleType
        else -> FTUnknownType
    }

    private fun FuncPrimitiveType?.toFt(): FTType =
        when (this?.firstChild?.elementType) {
            FuncElementTypes.INT_KEYWORD -> FTFuncPrimitiveType.INT
            FuncElementTypes.CELL_KEYWORD -> FTFuncPrimitiveType.CELL
            FuncElementTypes.BUILDER_KEYWORD -> FTFuncPrimitiveType.BUILDER
            FuncElementTypes.SLICE_KEYWORD -> FTFuncPrimitiveType.SLICE
            FuncElementTypes.TUPLE_KEYWORD -> FTFuncPrimitiveType.TUPLE
            FuncElementTypes.CONT_KEYWORD -> FTFuncPrimitiveType.CONT
            else -> FTUnknownType
        }

    private inner class DeclarationMapper(
        val expressionTreeMapper: ExpressionTreeMapper
    ) {
        fun FuncFunction.toFt() = FTFunction(
            returnType = FTTypeElement(typeReference.toFt()),
            name = FTNameIdentifier(nameIdentifier?.text ?: ""),
            typeParameterList = FTTypeParameterList(typeParameterList.map { it.toFt() }),
            functionModifiers = emptyList()
        )

        fun FuncTypeParameter.toFt() = FTTypeParameter(
            FTNameIdentifier(nameIdentifier?.text ?: "")
        )
    }

    private class ExpressionTreeMapper {
        fun FuncExpression?.toFT() {

        }
    }
}