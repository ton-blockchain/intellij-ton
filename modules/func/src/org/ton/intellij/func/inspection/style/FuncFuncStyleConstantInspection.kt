@file:Suppress("UnstableApiUsage")

package org.ton.intellij.func.inspection.style

import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo
import com.intellij.codeInspection.*
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.parentOfType
import org.ton.intellij.func.FuncBundle
import org.ton.intellij.func.inspection.FuncInspectionBase
import org.ton.intellij.func.psi.*

class FuncFuncStyleConstantInspection : FuncInspectionBase() {
    override fun buildFuncVisitor(
        holder: ProblemsHolder,
        session: LocalInspectionToolSession,
    ): FuncVisitor = object : FuncVisitor() {
        override fun visitFunction(function: FuncFunction) {
            if (!isFuncStyleConstant(function)) return

            val constantValue = extractConstantValue(function) ?: return

            holder.registerProblem(
                function.identifier,
                FuncBundle.message("inspection.func.style.constant.message"),
                ProblemHighlightType.WEAK_WARNING,
                FuncConvertToConstantFix(function, constantValue),
                FuncConvertAllToConstantsFix(function)
            )
        }
    }
}

class FuncConvertToConstantFix(
    element: PsiElement,
    private val constantValue: String,
) : LocalQuickFixAndIntentionActionOnPsiElement(element), LocalQuickFix {

    override fun getFamilyName() = FuncBundle.message("inspection.func.style.constant.fix.family.name")
    override fun getText() = FuncBundle.message("inspection.func.style.constant.fix.text", constantValue)
    override fun generatePreview(project: Project, editor: Editor, file: PsiFile): IntentionPreviewInfo = IntentionPreviewInfo.EMPTY

    override fun invoke(
        project: Project,
        file: PsiFile,
        editor: Editor?,
        startElement: PsiElement,
        endElement: PsiElement,
    ) {
        val function = startElement as? FuncFunction ?: return
        convertFunctionToConstant(project, function, constantValue)
    }
}

class FuncConvertAllToConstantsFix(
    element: PsiElement,
) : LocalQuickFixAndIntentionActionOnPsiElement(element), LocalQuickFix {

    override fun getFamilyName() = FuncBundle.message("inspection.func.style.constant.fix.all.family.name")
    override fun getText() = FuncBundle.message("inspection.func.style.constant.fix.all.text")
    override fun generatePreview(project: Project, editor: Editor, file: PsiFile): IntentionPreviewInfo = IntentionPreviewInfo.EMPTY

    override fun invoke(
        project: Project,
        file: PsiFile,
        editor: Editor?,
        startElement: PsiElement,
        endElement: PsiElement,
    ) {
        if (file !is FuncFile) return
        val functionsToConvert = mutableListOf<Pair<FuncFunction, String>>()

        for (function in file.functions) {
            if (isFuncStyleConstant(function)) {
                val value = extractConstantValue(function) ?: continue
                functionsToConvert.add(function to value)
            }
        }

        for ((function, value) in functionsToConvert) {
            convertFunctionToConstant(project, function, value)
        }
    }
}

private fun convertFunctionToConstant(project: Project, function: FuncFunction, constantValue: String) {
    val factory = FuncPsiFactory[project]
    val constantName = function.name ?: return

    val constantDecl = factory.createConstVarList(constantName, constantValue)

    ReferencesSearch.search(function).forEach { reference ->
        val callExpression = reference.element.parentOfType<FuncApplyExpression>()
        if (callExpression != null) {
            val referenceExpr = factory.createExpression(constantName)
            callExpression.replace(referenceExpr)
        }
    }

    function.replace(constantDecl)

    PsiDocumentManager.getInstance(project).commitAllDocuments()
}

private fun isFuncStyleConstant(function: FuncFunction): Boolean {
    val parameterList = function.functionParameterList
    if (parameterList.isNotEmpty()) {
        return false
    }

    val returnType = function.typeReference
    if (returnType.text != "int") {
        return false
    }

    val asmDefinition = function.asmDefinition ?: return false
    val asmBody = asmDefinition.asmBody ?: return false

    val stringLiterals = asmBody.stringLiteralList
    if (stringLiterals.size != 1) return false

    val content = stringLiterals.first().rawString?.text ?: return false
    return content.matches(Regex("\\s*0?x?[0-9a-fA-F]+\\s+PUSHINT\\s*"))
}

private fun extractConstantValue(function: FuncFunction): String? {
    val asmBody = function.asmDefinition?.asmBody ?: return null
    val content = asmBody.stringLiteralList.firstOrNull()?.rawString?.text ?: return null

    val hexMatch = Regex("\\s*(0?x?[0-9a-fA-F]+)\\s+PUSHINT\\s*").find(content)
    return hexMatch?.groupValues?.get(1)
}
