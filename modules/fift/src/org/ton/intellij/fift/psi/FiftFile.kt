package org.ton.intellij.fift.psi

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.FileViewProvider
import org.ton.intellij.fift.FiftFileType
import org.ton.intellij.fift.FiftLanguage

class FiftFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, FiftLanguage), FiftElement {
    override fun getFileType(): FileType = FiftFileType
    override fun toString(): String = "Fift"

    fun assemblyDefinitions(): List<FiftDefinition> {
        val roots = this.children.filterIsInstance<FiftAssembly>().mapNotNull { it.asmProgram }
        return roots.flatMap { it.children.filterIsInstance<FiftDefinition>() }
    }

    fun assemblyDeclarations(): List<FiftDeclaration> {
        val roots = this.children.filterIsInstance<FiftAssembly>().mapNotNull { it.asmProgram }
        return roots.flatMap { it.children.filterIsInstance<FiftDeclaration>() }
    }
}
