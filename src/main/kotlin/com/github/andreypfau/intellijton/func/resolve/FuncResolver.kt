package com.github.andreypfau.intellijton.func.resolve

import com.github.andreypfau.intellijton.func.FuncFileType
import com.github.andreypfau.intellijton.func.psi.*
import com.github.andreypfau.intellijton.psiManager
import com.intellij.psi.util.childrenOfType

fun FuncElement.resolveFile() = if (this is FuncFile) this else containingFile as FuncFile

fun FuncFile.resolveFunctions() = childrenOfType<FuncFunction>().asSequence()
fun FuncFile.resolveAllFunctions(): Sequence<FuncFunction> {
    val neighbourFiles = collectNeighbourFiles()
    val files = neighbourFiles + (resolveStdlibFile() ?: loadStdlib())
    return files.distinct().filterNotNull().flatMap { file ->
        file.resolveFunctions()
    }
}

fun FuncFile.resolveStdlibFile(): FuncFile? {
    if (name == "stdlib.fc") return this
    val virtualFile = virtualFile.parent.findChild("stdlib.fc") ?: return null
    return project.psiManager.findFile(virtualFile) as? FuncFile
}

private fun FuncFile.collectNeighbourFiles() = virtualFile.parent.children.asSequence().filter { file ->
    file.extension?.lowercase() in FuncFileType.extensions
}.map { file ->
    project.psiManager.findFile(file) as? FuncFile
}

fun FuncFunctionCall.resolveFunction() = reference?.resolve() as? FuncFunction
fun FuncMethodCall.resolveFunction() = reference?.resolve() as? FuncFunction
fun FuncModifyingMethodCall.resolveFunction() = reference?.resolve() as? FuncFunction

private fun FuncFile.loadStdlib(): FuncFile? = null
//    ReadOnlyLightVirtualFile(
//        "stdlib.func",
//        FuncLanguage,
//        loadTextResource(this, "func/stdlib.fc")
//    ).originalFile.let {
//        project.psiManager.findFile(it) as? FuncFile
//    }
