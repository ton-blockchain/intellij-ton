package org.ton.intellij.tlb.inspection

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.intellij.lang.annotations.Language
import org.ton.intellij.tlb.psi.TlbImplicitField
import org.ton.intellij.tlb.psi.TlbNamedElement
import org.ton.intellij.tlb.psi.TlbParamTypeExpression
import org.ton.intellij.tlb.psi.TlbResultType

class TlbUnresolvedReferenceInspectionTest : BasePlatformTestCase() {
    fun `test undeclared result type argument is reported`() {
        doInspectionTest(
            """
            nothing_with_counter#_ = ElemWithCounter 0 <error descr="Unresolved reference: X">X</error>;
            elem_with_counter#_ {n:#} {X:Type} value:X = ElemWithCounter (n + 1) X;
            """.trimIndent(),
        )
    }

    fun `test global type cannot satisfy bare result type argument`() {
        doInspectionTest(
            """
            x#_ = X;
            bad#_ = Foo <error descr="Unresolved reference: X">X</error>;
            """.trimIndent(),
        )
    }

    fun `test implicit type parameter satisfies result type argument`() {
        doInspectionTest(
            """
            nothing_with_counter#_ {X:Type} = ElemWithCounter 0 X;
            elem_with_counter#_ {n:#} {X:Type} value:X = ElemWithCounter (n + 1) X;
            """.trimIndent(),
        )
    }

    fun `test result type argument resolves to implicit field`() {
        val resolved = resolveAtCaret(
            """
            foo#_ {X:Type} = Foo <caret>X;
            """.trimIndent(),
        )

        assertTrue(resolved is TlbImplicitField)
        assertEquals("X", (resolved as TlbNamedElement).name)
    }

    fun `test field type resolves to global result type`() {
        val resolved = resolveAtCaret(
            """
            x#_ = X;
            wrapper#_ value:<caret>X = Wrapper;
            """.trimIndent(),
        )

        assertTrue(resolved is TlbResultType)
        assertEquals("X", (resolved as TlbNamedElement).name)
    }

    private fun doInspectionTest(
        @Language("TLB")
        text: String,
    ) {
        myFixture.enableInspections(TlbUnresolvedReferenceInspection())
        myFixture.configureByText("test.tlb", text)
        myFixture.testHighlighting()
    }

    private fun resolveAtCaret(
        @Language("TLB")
        text: String,
    ): PsiElement {
        val file = myFixture.configureByText("test.tlb", text)
        val expression = PsiTreeUtil.getParentOfType(
            file.findElementAt(myFixture.caretOffset),
            TlbParamTypeExpression::class.java,
            false,
        )
        assertNotNull(expression)

        return expression!!.reference?.resolve()
            ?: throw AssertionError("Expected `${expression.text}` to resolve")
    }
}
