package org.ton.intellij.tolk.documentation

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveElementVisitor
import org.ton.intellij.tolk.TolkTestBase
import org.ton.intellij.tolk.doc.TolkDocumentationProvider
import org.ton.intellij.tolk.psi.*
import java.io.File

class TolkDocumentationProviderTest : TolkTestBase() {
    private val provider = TolkDocumentationProvider()

    companion object {
        // Set to true to update expected HTML files automatically
        private const val UPDATE_EXPECTATIONS = false
    }

    override fun getTestDataPath(): String = "testResources/documentation"

    fun testConstVariableDocumentation() = doTest("const_var")
    fun testGlobalVariableDocumentation() = doTest("global_var")
    fun testFunctionDocumentation() = doTest("function")
    fun testStructDocumentation() = doTest("struct")
    fun testTypeDefDocumentation() = doTest("type_def")
    fun testParameterDocumentation() = doTest("parameter")
    fun testTypeParameterDocumentation() = doTest("type_parameter")
    fun testComplexTypesDocumentation() = doTest("complex_types")
    fun testUnionTypesDocumentation() = doTest("union_types")
    fun testGenericTypesDocumentation() = doTest("generic_types")
    fun testAnnotationsDocumentation() = doTest("annotations")
    fun testAsmFunctionsDocumentation() = doTest("asm_functions")
    fun testAdvancedTypesDocumentation() = doTest("advanced_types")
    fun testSelfTypesDocumentation() = doTest("self_types")
    fun testPrimitiveTypesDocumentation() = doTest("primitive_types")
    fun testDocCommentsDocumentation() = doTest("doc_comments")

    private fun doTest(testName: String) {
        val tolkFile = myFixture.configureByFile("$testName.tolk")
        val expectedFile = File("$testDataPath/$testName.expected.html")

        val documentationResults = mutableListOf<Pair<String, String>>()

        tolkFile.accept(object : PsiRecursiveElementVisitor() {
            override fun visitElement(element: PsiElement) {
                super.visitElement(element)

                val documentation = provider.generateDoc(element, null)
                if (documentation != null) {
                    val elementDescription = getElementDescription(element)
                    documentationResults.add(elementDescription to documentation)
                }
            }
        })

        val actualHtml = buildActualHtml(documentationResults)

        if (UPDATE_EXPECTATIONS) {
            expectedFile.parentFile?.mkdirs()
            expectedFile.writeText(actualHtml)
            println("Updated expectations for $testName")
        } else {
            if (!expectedFile.exists()) {
                fail("Expected file not found: ${expectedFile.absolutePath}. Set UPDATE_EXPECTATIONS = true to create it.")
            }

            val expectedHtml = expectedFile.readText()
            assertEquals("Documentation mismatch for $testName", expectedHtml.trim(), actualHtml.trim())
        }
    }

    private fun getElementDescription(element: PsiElement): String {
        return when (element) {
            is TolkFunction           -> "Function: ${element.name}"
            is TolkConstVar           -> "Const: ${element.name}"
            is TolkGlobalVar          -> "Global: ${element.name}"
            is TolkStruct             -> "Struct: ${element.name}"
            is TolkStructField        -> "Field: ${element.name}"
            is TolkTypeDef            -> "Type: ${element.name}"
            is TolkParameter          -> "Parameter: ${element.name}"
            is TolkVar                -> "Variable: ${element.name}"
            is TolkTypeParameter      -> "Type param: ${element.name}"
            is TolkCatchParameter     -> "Catch param: ${element.name}"
            is TolkAnnotation         -> "Annotation: ${element.identifier?.text ?: "unknown"}"
            is TolkSelfParameter      -> "Self param: ${if (element.isMutable) "mutate " else ""}self"
            is TolkSelfTypeExpression -> "Self type"
            is TolkTypeExpression     -> "Type expression: ${element.text}"
            else                      -> element.javaClass.simpleName
        }
    }

    private fun buildActualHtml(results: List<Pair<String, String>>): String {
        return buildString {
            appendLine("<!DOCTYPE html>")
            appendLine("<html>")
            appendLine("<head>")
            appendLine("    <meta charset=\"UTF-8\">")
            appendLine("    <title>Tolk Documentation Test Results</title>")
            appendLine("    <style>")
            appendLine("        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 20px; background: #fafafa; }")
            appendLine("        h1 { color: #2c3e50; border-bottom: 3px solid #3498db; padding-bottom: 10px; }")
            appendLine("        .element { margin-bottom: 25px; border: 1px solid #ddd; padding: 20px; background: white; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }")
            appendLine("        .element-title { font-weight: bold; color: #2c3e50; margin-bottom: 15px; font-size: 14px; background: #ecf0f1; padding: 8px; border-radius: 4px; }")
            appendLine("        .documentation { background: #f8f9fa; padding: 15px; border-radius: 6px; border-left: 4px solid #3498db; font-family: 'JetBrains Mono', 'Fira Code', monospace; line-height: 1.6; }")
            appendLine("        ")
            appendLine("        /* Tolk syntax highlighting */")
            appendLine("        .keyword { color: #d73a49; font-weight: bold; }")
            appendLine("        .primitive { color: #005cc5; font-weight: 500; }")
            appendLine("        .struct { color: #22863a; font-weight: 500; }")
            appendLine("        .function { color: #6f42c1; font-weight: 500; }")
            appendLine("        .parameter { color: #e36209; }")
            appendLine("        .type-alias { color: #d73a49; }")
            appendLine("        .type-parameter { color: #6f42c1; font-style: italic; }")
            appendLine("        .annotation { color: #6a737d; }")
            appendLine("        .string { color: #032f62; }")
            appendLine("        .number { color: #005cc5; }")
            appendLine("        .comment { color: #6a737d; font-style: italic; }")
            appendLine("        .constant { color: #005cc5; font-weight: 500; }")
            appendLine("        .global-variable { color: #e36209; font-weight: 500; }")
            appendLine("        .identifier { color: #24292e; }")
            appendLine("        ")
            appendLine("        /* Documentation markup */")
            appendLine("        .definition-start { border-top: 2px solid #e1e4e8; margin-top: 10px; padding-top: 10px; }")
            appendLine("        .definition-end { border-bottom: 1px solid #e1e4e8; margin-bottom: 10px; padding-bottom: 10px; }")
            appendLine("        .content-start { margin-top: 15px; }")
            appendLine("    </style>")
            appendLine("</head>")
            appendLine("<body>")
            appendLine("    <h1>Tolk Documentation Test Results</h1>")

            results.forEach { (elementDescription, documentation) ->
                appendLine("    <div class=\"element\">")
                appendLine("        <div class=\"element-title\">$elementDescription</div>")
                appendLine("        <div class=\"documentation\">")
                appendLine("            $documentation")
                appendLine("        </div>")
                appendLine("    </div>")
            }

            appendLine("</body>")
            appendLine("</html>")
        }
    }
}
