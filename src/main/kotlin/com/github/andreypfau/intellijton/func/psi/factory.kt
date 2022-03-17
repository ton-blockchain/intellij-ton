package com.github.andreypfau.intellijton.func.psi

import com.github.andreypfau.intellijton.childOfType
import com.github.andreypfau.intellijton.func.FuncFileType
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory

val Project.funcPsiFactory get() = FuncPsiFactory(this)

class FuncPsiFactory(val project: Project) {

    fun createFile(code: CharSequence): FuncFile = createFromText(code) ?: error("Failed to create file: `$code`")

    fun createIdentifier(name: String): PsiElement =
        createFromText<FuncFunction>("() $name();")?.functionName?.identifier
            ?: error("Failed to create identifier: `$name`")

    inline fun <reified T : FuncElement> createFromText(code: CharSequence): T? = createFromText(code, T::class.java)

    fun <T : FuncElement> createFromText(code: CharSequence, type: Class<T>): T? =
        PsiFileFactory.getInstance(project)
            .createFileFromText("DUMMY.${FuncFileType.defaultExtension}", FuncFileType, code)
            .childOfType(type)
}