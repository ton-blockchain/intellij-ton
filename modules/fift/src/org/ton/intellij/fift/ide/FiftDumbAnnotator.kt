package org.ton.intellij.fift.ide

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import org.ton.intellij.fift.psi.FiftDeclaration
import org.ton.intellij.fift.psi.FiftDefinitionName
import org.ton.intellij.fift.psi.FiftTvmInstruction
import org.ton.intellij.fift.psi.FiftTypes

class FiftDumbAnnotator : Annotator, DumbAware {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        val elementType = element.elementType ?: return
        val color = when (elementType) {
            FiftTypes.IDENTIFIER -> {
                val parent = element.parent
                identifierColor(parent) ?: return
            }

            else                 -> return
        }
        holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
            .textAttributes(color.textAttributesKey)
            .create()
    }
}

fun identifierColor(element: PsiElement): FiftColor? {
    return when (element) {
        is FiftTvmInstruction -> {
            val firstChar = element.text[0]
            if (firstChar !in 'A'..'Z' && firstChar !in '0'..'9' && firstChar != '-') {
                // foo CALLDICT
                // ^^^
                return FiftColor.ASSEMBLY_CALL
            }
            return FiftColor.ASSEMBLY_INSTRUCTION
        }

        else                  -> {
            val grand = element.parent
            if (grand is FiftDefinitionName) {
                return FiftColor.ASSEMBLY_DEFINITION
            }
            if (grand is FiftDeclaration) {
                return FiftColor.ASSEMBLY_DEFINITION
            }
            null
        }
    }
}