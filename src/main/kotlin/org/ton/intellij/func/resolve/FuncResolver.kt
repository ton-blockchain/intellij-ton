package org.ton.intellij.func.resolve

import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.childrenOfType
import com.intellij.psi.util.parentsOfType
import com.intellij.testFramework.ReadOnlyLightVirtualFile
import org.ton.intellij.childOfType
import org.ton.intellij.collectElements
import org.ton.intellij.func.FuncFileType
import org.ton.intellij.func.FuncIcons
import org.ton.intellij.func.FuncLanguage
import org.ton.intellij.func.psi.*
import org.ton.intellij.loadTextResource
import org.ton.intellij.psiManager
import kotlin.io.path.relativeTo

private val stdlib by lazy {
    ReadOnlyLightVirtualFile(
        "stdlib.fc",
        FuncLanguage,
        loadTextResource(FuncFile::class.java, "func/stdlib.fc")
    )
}

private fun FuncElement.stdlib(): FuncFile? {
    val virtualFile = stdlib
    project.funcPsiFactory.project
    val viewProvider = PsiManager.getInstance(project).findViewProvider(virtualFile) ?: return null
    return viewProvider.getPsi(FuncLanguage) as? FuncFile
}

fun FuncElement.resolveFile() = if (this is FuncFile) this else containingFile as FuncFile

fun FuncFile.resolveFunctions() = childrenOfType<FuncFunction>().asSequence()
fun FuncFile.resolveAllFunctions(): Sequence<FuncFunction> {
    // TODO: optimise via stub-indexes
    val allFiles = collectFiles()
    val stdlib = FuncInternalFactory[project].stdLib
    val files = buildList {
        add(stdlib)
        addAll(allFiles)
    }
    val functions = files.flatMap { file ->
        file.resolveFunctions()
    }.distinctBy {
        it.toString()
    }
    return functions.asSequence()
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

fun FuncFunction.resolveParameters() = parameterList?.parameterDeclarationList?.asSequence() ?: emptySequence()

private fun FuncFile.collectNeighbourFiles(): Sequence<FuncFile> =
    virtualFile?.parent?.children?.asSequence()?.filter { file ->
        file.extension?.lowercase() in FuncFileType.extensions
    }?.mapNotNull { file ->
        project.psiManager.findFile(file) as? FuncFile
    } ?: emptySequence()

fun FuncFunctionCall.resolveFunction() = reference?.resolve() as? FuncFunction
fun FuncMethodCall.resolveFunction() = reference?.resolve() as? FuncFunction
fun FuncModifyingMethodCall.resolveFunction() = reference?.resolve() as? FuncFunction


fun FuncElement.completeFiles(): List<LookupElementBuilder> {
    val currentFilePath = containingFile.originalFile.virtualFile.toNioPath()
    val files = collectFiles()
    return files.mapNotNull {
        val path = it.originalFile.virtualFile.toNioPath().relativeTo(currentFilePath).toString()
        if (path.length > 3) {
            LookupElementBuilder.create(path.substring(3))
                .withIcon(FuncIcons.FILE)
        } else null
    }
}

fun FuncElement.collectFiles(): List<FuncFile> {
    val project = project
    val files =
        FilenameIndex.getAllFilesByExt(project, "fc") +
                FilenameIndex.getAllFilesByExt(project, "func")
    return files.mapNotNull {
        project.psiManager.findFile(it) as? FuncFile
    }
}