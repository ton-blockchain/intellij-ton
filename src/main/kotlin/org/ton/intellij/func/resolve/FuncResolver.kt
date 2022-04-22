package org.ton.intellij.func.resolve

import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.childrenOfType
import com.intellij.psi.util.parentsOfType
import org.ton.intellij.childOfType
import org.ton.intellij.collectElements
import org.ton.intellij.func.FuncFileType
import org.ton.intellij.func.psi.*
import org.ton.intellij.psiManager

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

fun FuncFile.resolveReferenceExpressionProviders(offset: Int): Sequence<FuncNamedElement> {
    val function = resolveFunctions().find { it.textRange.contains(offset) }
    val functionExpressionProviders = if (function != null) {
        function.resolveParameters() + function.resolveVariables(offset).flattenVariables()
    } else emptySequence()
    return resolveConstants() + resolveGlobalVars() + functionExpressionProviders
}

fun FuncFile.resolveGlobalVars() = childrenOfType<FuncGlobalVarExpression>().asSequence().map {
    it.globalVarList
}.flatten()

fun FuncFile.resolveConstants() = childrenOfType<FuncConstExpression>().asSequence().map {
    it.constDeclarationList
}.flatten()

fun FuncFunction.resolveVariables(offset: Int): Sequence<FuncVariableDeclaration> {
    val currentBlock =
        PsiTreeUtil.findElementOfClassAtOffset(containingFile, offset, FuncBlockStatement::class.java, false)!!
    val currentLevel = currentBlock.parentsOfType<FuncBlockStatement>().count()
    return collectElements<FuncVariableDeclaration>()
        .asSequence()
        .filter { it.textOffset < offset }
        .filter {
            it.parentsOfType<FuncBlockStatement>().count() <= currentLevel
        }
}

fun Sequence<FuncVariableDeclaration>.flattenVariables() = mapNotNull {
    if (it.identifier != null) {
        sequenceOf(it)
    } else if (it.tensorExpression != null) {
        it.tensorExpression?.tensorExpressionItemList?.asSequence()?.mapNotNull {
            it.childOfType<FuncReferenceExpression>()
        }
    } else null
}.flatten()

fun FuncFunction.resolveParameters() = parameterList.parameterDeclarationList.asSequence()

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
