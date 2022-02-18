package com.github.andreypfau.intellijton.func.resolve

import com.github.andreypfau.intellijton.func.psi.FuncElement
import com.github.andreypfau.intellijton.func.psi.FuncFile
import com.github.andreypfau.intellijton.func.psi.FuncFunction
import com.github.andreypfau.intellijton.psiManager
import com.intellij.psi.util.childrenOfType
import com.intellij.psi.util.findParentOfType

fun FuncElement.resolveFile() = if (this is FuncFile) this else findParentOfType()!!
fun FuncFile.resolveFunctions() = childrenOfType<FuncFunction>().asSequence()
fun FuncFile.resolveStdlibFile(): FuncFile? {
    if (name == "stdlib.fc") return this
    val virtualFile = virtualFile.parent.findChild("stdlib.fc") ?: return null
    return project.psiManager.findFile(virtualFile) as? FuncFile
}

fun FuncFile.resolveAllFunctions() =
    (resolveStdlibFile()?.resolveFunctions() ?: emptySequence()) + resolveFunctions()