package com.github.andreypfau.intellijton.func.psi

import com.github.andreypfau.intellijton.childOfType
import com.github.andreypfau.intellijton.func.FuncFileType
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory

val Project.funcPsiFactory get() = FuncPsiFactory(this)

class FuncPsiFactory(val project: Project) {

    fun createIdentifier(name: String): PsiElement =
        createFromText<FuncFunctionDefinition>("() $name();")?.nameIdentifier
            ?: error("Failed to create identifier: `$name`")

    fun createFunction(code: CharSequence): FuncFunctionDefinition =
        createFromText(code) ?: error("Failed to create function: `$code`")

    fun createBuiltInFunction(code: CharSequence) = createFunction(code).also {
        it as FuncFunctionDefinitionMixin
        it.isBuiltIn = true
    }

    private inline fun <reified T : FuncElement> createFromText(code: CharSequence): T? =
        PsiFileFactory.getInstance(project)
            .createFileFromText("DUMMY.fc", FuncFileType, code)
            .childOfType()
}