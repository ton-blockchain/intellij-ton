package org.ton.intellij.fift.psi

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.util.PsiTreeUtil
import org.ton.intellij.fift.FiftFileType
import org.ton.intellij.fift.resolve.FiftReferenceBase
import org.ton.intellij.fift.resolve.resolveFile

class FiftAssemblyReference(element: FiftTvmInstruction) : FiftReferenceBase<FiftTvmInstruction>(element) {
    override fun multiResolve(): Sequence<PsiElement> {
        val name = element.text
        val file = element.resolveFile()

        if (!element.isNotInstruction()) return emptySequence()

        val definitions = file.assemblyDefinitions().filter { it.name() == name }.map { it.firstChild.firstChild.firstChild }
        val declarations =
            file.assemblyDeclarations().filter { it.globalVar != null && it.name() == name }.map { it.lastChild.lastChild.firstChild }
        return (definitions + declarations).asSequence()
    }

    override fun handleElementRename(newElementName: String): PsiElement {
        val newNameElement = PsiFileFactory.getInstance(element.project)
            .createFileFromText(
                "dummy.fif", FiftFileType, """
                PROGRAM{
                    foo PROCINLINE:<{
                        $newElementName CALLDICT
                    }>
                }
            """.trimIndent()
            )

        val instr = PsiTreeUtil.findChildrenOfAnyType(newNameElement, FiftTvmInstruction::class.java).firstOrNull()
        if (instr != null) {
            element.replace(instr)
        }
        return newNameElement
    }
}
