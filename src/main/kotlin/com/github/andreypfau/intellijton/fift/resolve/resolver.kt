package com.github.andreypfau.intellijton.fift.resolve

import com.github.andreypfau.intellijton.fift.psi.FiftElement
import com.github.andreypfau.intellijton.fift.psi.FiftFile
import com.github.andreypfau.intellijton.fift.psi.FiftWordDefStatement
import com.github.andreypfau.intellijton.psiManager
import com.intellij.psi.util.childrenOfType
import com.intellij.psi.util.findParentOfType

fun FiftElement.resolveFile() = if (this is FiftFile) this else findParentOfType()!!

fun FiftFile.resolveStdlibFile(): FiftFile? {
    if (name == "Fift.fif") return this
    val virtualFile = virtualFile.parent.findChild("Fift.fif") ?: return null
    return project.psiManager.findFile(virtualFile) as? FiftFile
}

fun FiftFile.resolveWordDefStatements() = childrenOfType<FiftWordDefStatement>().asSequence()

fun FiftFile.resolveAllWordDefStatements() =
    (resolveStdlibFile()?.resolveWordDefStatements() ?: emptySequence()) + resolveWordDefStatements()