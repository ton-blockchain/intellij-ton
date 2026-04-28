package org.ton.intellij.tolk.psi

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.util.PsiTreeUtil
import org.ton.intellij.tolk.TolkTestBase
import org.ton.intellij.tolk.doc.TolkDocumentationProvider

class TolkAnnotationPsiTest : TolkTestBase() {
    fun `test dotted annotation name is exposed as full text`() {
        myFixture.configureByText(
            "test.tolk",
            """
                @abi.minimalMsgValue(1)
                struct Message {}
            """.trimIndent(),
        )

        val annotation = PsiTreeUtil.findChildOfType(myFixture.file, TolkAnnotation::class.java)

        assertNotNull(annotation)
        assertEquals("abi.minimalMsgValue", annotation!!.name)
        assertEquals("abi.minimalMsgValue", annotation.annotationName?.text)
    }

    fun `test annotation query matches dotted annotation names`() {
        myFixture.configureByText(
            "test.tolk",
            """
                @abi.minimalMsgValue(1)
                struct Message {}
            """.trimIndent(),
        )

        val struct = PsiTreeUtil.findChildOfType(myFixture.file, TolkStruct::class.java)

        assertNotNull(struct)
        assertTrue(struct!!.annotations.hasAnnotation("abi.minimalMsgValue"))
        assertContainsElements(struct.annotations.names().toList(), "abi.minimalMsgValue")
    }

    fun `test dotted annotation documentation falls back to root annotation info`() {
        val file = myFixture.configureByText(
            "test.tolk",
            """
                @abi.minimalMsgValue(1) struct Message {}
            """.trimIndent(),
        )

        val annotation = PsiTreeUtil.findChildOfType(file, TolkAnnotation::class.java)

        assertNotNull(annotation)

        val documentation = TolkDocumentationProvider().generateDoc(annotation, null)

        assertNotNull(documentation)
        assertTrue(documentation!!.contains("Describes ABI metadata for declaration."))
    }

    fun `test dotted test annotation documentation uses exact mapping doc`() {
        val file = myFixture.configureByText(
            "test.tolk",
            """
                @test.fuzz({ runs: 4 }) get fun `test fuzzed`(value: int) {}
            """.trimIndent(),
        )

        val annotation = PsiTreeUtil.findChildOfType(file, TolkAnnotation::class.java)

        assertNotNull(annotation)

        val documentation = TolkDocumentationProvider().generateDoc(annotation, null)

        assertNotNull(documentation)
        assertTrue(documentation!!.contains("@test.fuzz"))
        assertTrue(documentation.contains("@test.fuzz(64)"))
        assertTrue(documentation.contains("@test.fuzz({ ... })"))
    }

    fun `test hover on dotted annotation segments resolves to annotation docs`() {
        val file = myFixture.configureByText(
            "test.tolk",
            """
                @test.skip get fun `test skipped`() {}
            """.trimIndent(),
        )

        val provider = TolkDocumentationProvider()

        listOf("test", ".", "skip").forEach { marker ->
            val offset = file.text.indexOf(marker)
            assertTrue(offset >= 0)

            val contextElement = file.findElementAt(offset)
            val target = provider.getCustomDocumentationElement(myFixture.editor, file, contextElement, offset)

            assertTrue(target is TolkAnnotation)

            val documentation = provider.generateDoc(target, contextElement)

            assertNotNull(documentation)
            assertTrue(documentation!!.contains("Marks the test as skipped."))
        }
    }

    fun `test dotted annotation name is reformatted without spaces around dots`() {
        val file = myFixture.configureByText(
            "test.tolk",
            """
                @abi . minimalMsgValue(1)struct Message {}
            """.trimIndent(),
        )

        WriteCommandAction.runWriteCommandAction(project) {
            CodeStyleManager.getInstance(project).reformat(file)
        }

        assertTrue(file.text.contains("@abi.minimalMsgValue(1) struct Message {"))
        assertFalse(file.text.contains("@abi . minimalMsgValue"))
        assertFalse(file.text.contains("@abi. minimalMsgValue"))
        assertFalse(file.text.contains("@abi .minimalMsgValue"))
    }

    fun `test struct field annotations are exposed through annotation holder`() {
        myFixture.configureByText(
            "test.tolk",
            """
                struct Message {
                    @deprecated("use body")
                    body: int
                }
            """.trimIndent(),
        )

        val field = PsiTreeUtil.findChildOfType(myFixture.file, TolkStructField::class.java)

        assertNotNull(field)
        assertTrue(field!!.annotations.hasAnnotation("deprecated"))
        assertContainsElements(field.annotations.names().toList(), "deprecated")
    }

    fun `test annotation argument can be parsed as type expression`() {
        val file = myFixture.configureByText(
            "test.tolk",
            """
                @abi.clientType(PayloadInline | PayloadRef)
                struct Message {}
            """.trimIndent(),
        )

        val annotation = PsiTreeUtil.findChildOfType(file, TolkAnnotation::class.java)

        assertNotNull(annotation)
        assertEquals("abi.clientType", annotation!!.name)
        assertEquals(1, annotation.arguments.size)
        assertEquals("PayloadInline | PayloadRef", annotation.arguments.single().text)
        assertNotNull(annotation.arguments.single().typeExpression)
        assertNull(annotation.arguments.single().expression)

        assertNotNull(PsiTreeUtil.findChildOfType(annotation, TolkTypeExpression::class.java))
        assertNull(PsiTreeUtil.findChildOfType(annotation, TolkExpression::class.java))
    }
}
