package org.ton.intellij.fift.resolve

import com.intellij.psi.util.childrenOfType
import com.intellij.psi.util.findParentOfType
import org.ton.intellij.fift.psi.FiftElement
import org.ton.intellij.fift.psi.FiftFile
import org.ton.intellij.fift.psi.FiftWordDefStatement
import org.ton.intellij.psiManager

fun FiftElement.resolveFile() = if (this is FiftFile) this else findParentOfType()!!

fun FiftFile.resolveStdlibFile(): FiftFile? {
    if (name == "Fift.fif") return this
    val virtualFile = virtualFile.parent.findChild("Fift.fif") ?: return null
    return project.psiManager.findFile(virtualFile) as? FiftFile
}

fun FiftFile.resolveWordDefStatements() = childrenOfType<FiftWordDefStatement>().asSequence()

fun FiftFile.resolveAllWordDefStatements() =
    (resolveStdlibFile()?.resolveWordDefStatements() ?: emptySequence()) + resolveWordDefStatements()