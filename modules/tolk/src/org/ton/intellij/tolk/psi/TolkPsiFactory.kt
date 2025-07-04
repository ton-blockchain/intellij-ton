package org.ton.intellij.tolk.psi

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiParserFacade
import org.ton.intellij.tolk.TolkLanguage
import org.ton.intellij.util.descendantOfTypeStrict

@Service(Service.Level.PROJECT)
class TolkPsiFactory private constructor(val project: Project) {
    fun createFile(text: CharSequence) =
        createFile(null, text)

    fun createFile(name: String?, text: CharSequence) =
        PsiFileFactory.getInstance(project).createFileFromText(name ?: "dummy.tolk", TolkLanguage, text) as TolkFile

    fun createNewline(): PsiElement = createWhitespace("\n")

    fun createWhitespace(ws: String): PsiElement =
        PsiParserFacade.getInstance(project).createWhiteSpaceFromText(ws)

    fun createStringLiteral(text: String): TolkStringLiteral {
        if (text.contains("\n")) {
            return (createExpression("\"\"\"$text\"\"\"") as TolkLiteralExpression).stringLiteral!!
        } else {
            return (createExpression("\"$text\"") as TolkLiteralExpression).stringLiteral!!
        }
    }

    fun createStatement(text: String): TolkStatement {
        val file = createFile("fun foo() { $text }")
        return file.functions.first().functionBody!!.blockStatement!!.statementList.first()
    }

    fun createExpression(text: String): TolkExpression {
        return (createStatement("$text;") as TolkExpressionStatement).expression
    }

    fun createIdentifier(text: String): PsiElement {
        val funcFile = createFile("fun $text() {}")
        val function = funcFile.functions.first()
        return function.identifier!!
    }

    fun createIncludeDefinition(text: String): TolkIncludeDefinition =
        createFromText("import \"$text\";")
            ?: error("Failed to create include definition from text: `$text`")

    private inline fun <reified T : TolkElement> createFromText(
        code: CharSequence
    ): T? = createFile(code).descendantOfTypeStrict()

    companion object {
        operator fun get(project: Project) =
            requireNotNull(project.getService(TolkPsiFactory::class.java))
    }
}

val Project.tolkPsiFactory: TolkPsiFactory get() = TolkPsiFactory[this]
