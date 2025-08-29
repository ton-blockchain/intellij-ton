package org.ton.intellij.func.inspection.style

import com.intellij.codeInspection.*
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.ton.intellij.func.FuncBundle
import org.ton.intellij.func.inspection.FuncInspectionBase
import org.ton.intellij.func.psi.*

class FuncMissingImpureInspection : FuncInspectionBase() {
    override fun buildFuncVisitor(
        holder: ProblemsHolder,
        session: LocalInspectionToolSession,
    ): FuncVisitor = object : FuncVisitor() {
        override fun visitFunction(function: FuncFunction) {
            if (shouldHaveImpureSpecifier(function)) {
                val identifier = function.identifier
                holder.registerProblem(
                    identifier,
                    FuncBundle.message("inspection.func.missing.impure.message"),
                    ProblemHighlightType.WEAK_WARNING,
                    FuncAddImpureFix(function)
                )
            }
        }
    }

    private fun shouldHaveImpureSpecifier(function: FuncFunction): Boolean {
        if (hasSpecifier(function, "impure") || hasSpecifier(function, "method_id")) {
            return false
        }
        if (function.name == "main" || function.name == "recv_internal" || function.name == "recv_external") {
            return false
        }
        if (function.asmDefinition != null) {
            return false
        }

        val returnType = function.typeReference
        return returnsUnitType(returnType)
    }

    private fun hasSpecifier(function: FuncFunction, name: String): Boolean {
        var current: PsiElement? = function.firstChild
        while (current != null) {
            if (current.text == name) {
                return true
            }
            current = current.nextSibling
        }
        return false
    }

    private fun returnsUnitType(typeReference: FuncTypeReference?): Boolean {
        if (typeReference is FuncUnitType) {
            return true
        }
        return typeReference?.text == "()"
    }
}

class FuncAddImpureFix(
    element: PsiElement,
) : LocalQuickFixAndIntentionActionOnPsiElement(element), LocalQuickFix {

    override fun getFamilyName(): String = FuncBundle.message("inspection.func.missing.impure.fix.family.name")
    override fun getText(): String = FuncBundle.message("inspection.func.missing.impure.fix.text")

    override fun invoke(
        project: Project,
        file: PsiFile,
        editor: Editor?,
        startElement: PsiElement,
        endElement: PsiElement,
    ) {
        val function = startElement as? FuncFunction ?: return
        addImpureSpecifier(project, function)
    }

    private fun addImpureSpecifier(project: Project, function: FuncFunction) {
        val functionBody = function.blockStatement ?: function.asmDefinition

        val factory = FuncPsiFactory[project]
        val impureKeyword = factory.createModifier("impure")
        val space = factory.createWhitespace(" ")

        if (functionBody != null) {
            function.addBefore(space, functionBody)
            function.addBefore(impureKeyword, functionBody)
        } else {
            val identifier = function.identifier
            var current: PsiElement? = identifier.nextSibling
            while (current != null) {
                if (current.text == ";") {
                    function.addBefore(space, current)
                    function.addBefore(impureKeyword, current)
                    break
                }
                current = current.nextSibling
            }
        }
    }
}
