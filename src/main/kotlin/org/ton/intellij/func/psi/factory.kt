package org.ton.intellij.func.psi

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiParserFacade
import org.ton.intellij.childOfType
import org.ton.intellij.func.FuncFileType

val Project.funcPsiFactory get() = FuncInternalFactory[this].psiFactory

class FuncPsiFactory(val project: Project) {

    fun createFile(code: CharSequence, fileName: String = "DUMMY", eventSystemEnabled: Boolean = false): FuncFile =
        PsiFileFactory.getInstance(project)
            .createFileFromText(
                "$fileName.${FuncFileType.defaultExtension}",
                FuncFileType,
                code,
                System.currentTimeMillis(),
                eventSystemEnabled,
                false
            ) as FuncFile

    fun createIdentifier(name: String): PsiElement =
        createFromText<FuncFunction>("() $name();").functionName.identifier

    fun createIncludeDirective(includePath: String): FuncIncludeDirective =
        createFromText("#include \"$includePath\";")

    fun createNewLine() = PsiParserFacade.SERVICE.getInstance(project).createWhiteSpaceFromText("\n")

    inline fun <reified T : FuncElement> createFromText(code: CharSequence): T = createFromText(code, T::class.java)

    fun <T : FuncElement> createFromText(code: CharSequence, type: Class<T>): T =
        requireNotNull(createFile(code).childOfType(type, strict = false)) {
            "Failed create $type for `$code`"
        }
}