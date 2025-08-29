@file:Suppress("UnstableApiUsage")

package org.ton.intellij.tolk.inspection.style

import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo
import com.intellij.codeInspection.*
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.parentOfType
import org.ton.intellij.tolk.TolkBundle
import org.ton.intellij.tolk.inspection.TolkInspectionBase
import org.ton.intellij.tolk.psi.*

class TolkFuncStyleConstantInspection : TolkInspectionBase() {
    override fun buildTolkVisitor(
        holder: ProblemsHolder,
        session: LocalInspectionToolSession,
    ): TolkVisitor = object : TolkVisitor() {
        override fun visitFunction(function: TolkFunction) {
            if (!isFuncStyleConstant(function)) return

            val constantValue = extractConstantValue(function) ?: return

            holder.registerProblem(
                function.identifier ?: function,
                TolkBundle.message("inspection.func.style.constant.message"),
                ProblemHighlightType.WARNING,
                TolkConvertToConstantFix(function, constantValue),
                TolkConvertAllToConstantsFix(function)
            )
        }
    }
}

class TolkConvertToConstantFix(
    element: PsiElement,
    private val constantValue: String,
) : LocalQuickFixAndIntentionActionOnPsiElement(element), LocalQuickFix {

    override fun getFamilyName() = TolkBundle.message("inspection.func.style.constant.fix.family.name")
    override fun getText() = TolkBundle.message("inspection.func.style.constant.fix.text", constantValue)
    override fun generatePreview(project: Project, editor: Editor, file: PsiFile): IntentionPreviewInfo = IntentionPreviewInfo.EMPTY

    override fun invoke(
        project: Project,
        file: PsiFile,
        editor: Editor?,
        startElement: PsiElement,
        endElement: PsiElement,
    ) {
        val function = startElement as? TolkFunction ?: return
        convertFunctionToConstant(project, function, constantValue)
    }
}

class TolkConvertAllToConstantsFix(
    element: PsiElement,
) : LocalQuickFixAndIntentionActionOnPsiElement(element), LocalQuickFix {

    override fun getFamilyName() = TolkBundle.message("inspection.func.style.constant.fix.all.family.name")
    override fun getText() = TolkBundle.message("inspection.func.style.constant.fix.all.text")
    override fun generatePreview(project: Project, editor: Editor, file: PsiFile): IntentionPreviewInfo = IntentionPreviewInfo.EMPTY

    override fun invoke(
        project: Project,
        file: PsiFile,
        editor: Editor?,
        startElement: PsiElement,
        endElement: PsiElement,
    ) {
        if (file !is TolkFile) return
        val functionsToConvert = mutableListOf<Pair<TolkFunction, String>>()

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

private fun convertFunctionToConstant(project: Project, function: TolkFunction, constantValue: String) {
    val factory = TolkPsiFactory[project]
    val constantName = function.name ?: return

    val constantDecl = factory.createConstant(constantName, constantValue)

    ReferencesSearch.search(function).forEach { reference ->
        val callExpression = reference.element.parentOfType<TolkCallExpression>()
        if (callExpression != null) {
            val referenceExpr = factory.createExpression(constantName)
            callExpression.replace(referenceExpr)
        }
    }

    // replace function with new plain constant
    function.replace(constantDecl)

    PsiDocumentManager.getInstance(project).commitAllDocuments()
}

private fun isFuncStyleConstant(function: TolkFunction): Boolean {
    val parameterList = function.parameterList
    if (parameterList?.parameterList?.isNotEmpty() == true || parameterList?.selfParameter != null) {
        return false
    }

    val returnType = function.returnType?.typeExpression
    if (returnType?.text != "int") {
        return false
    }

    val asmDefinition = function.functionBody?.asmDefinition ?: return false
    val asmBody = asmDefinition.asmBody ?: return false

    val stringLiterals = asmBody.stringLiteralList
    if (stringLiterals.size != 1) return false

    val content = stringLiterals.first().rawString?.text ?: return false
    return content.matches(Regex("\\s*0?x?[0-9a-fA-F]+\\s+PUSHINT\\s*"))
}

private fun extractConstantValue(function: TolkFunction): String? {
    val asmBody = function.functionBody?.asmDefinition?.asmBody ?: return null
    val content = asmBody.stringLiteralList.firstOrNull()?.rawString?.text ?: return null

    val hexMatch = Regex("\\s*(0?x?[0-9a-fA-F]+)\\s+PUSHINT\\s*").find(content)
    return hexMatch?.groupValues?.get(1)
}
