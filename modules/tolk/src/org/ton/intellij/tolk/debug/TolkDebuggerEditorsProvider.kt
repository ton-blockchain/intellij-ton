package org.ton.intellij.tolk.debug

import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.xdebugger.XDebuggerUtil
import com.intellij.xdebugger.XExpression
import com.intellij.xdebugger.XSourcePosition
import com.intellij.xdebugger.XDebuggerManager
import com.intellij.xdebugger.evaluation.EvaluationMode
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProviderBase
import org.ton.intellij.tolk.TolkFileType
import org.ton.intellij.tolk.TolkLanguage
import org.ton.intellij.tolk.psi.TolkExpressionCodeFragment

object TolkDebuggerEditorsProvider : XDebuggerEditorsProviderBase() {
    override fun getFileType(): FileType = TolkFileType

    @Suppress("OVERRIDE_DEPRECATION")
    override fun createDocument(
        project: Project,
        text: String,
        sourcePosition: XSourcePosition?,
        mode: EvaluationMode
    ): Document {
        val expression = XDebuggerUtil.getInstance().createExpression(text, TolkLanguage, null, mode)
        return createDocument(project, expression, sourcePosition, mode)
    }

    override fun createDocument(
        project: Project,
        expression: XExpression,
        sourcePosition: XSourcePosition?,
        mode: EvaluationMode
    ): Document {
        val document = super.createDocument(project, expression, sourcePosition, mode)
        installDebugCompletionContext(project, document)
        return document
    }

    override fun createDocument(
        project: Project,
        expression: XExpression,
        context: PsiElement?,
        mode: EvaluationMode
    ): Document {
        val document = super.createDocument(project, expression, context, mode)
        installDebugCompletionContext(project, document)
        return document
    }

    override fun createExpressionCodeFragment(
        project: Project,
        text: String,
        context: PsiElement?,
        isPhysical: Boolean
    ): PsiFile = TolkExpressionCodeFragment(project, "debug-evaluate.tolk", text, isPhysical).apply {
        setContext(context)
        context?.resolveScope?.let { forceResolveScope(it) }
    }

    private fun installDebugCompletionContext(project: Project, document: Document) {
        val currentStackFrame = XDebuggerManager.getInstance(project).currentSession?.currentStackFrame as? TolkDapXStackFrame
        TolkDebugEvaluateCompletion.install(
            document,
            TolkDebugEvaluateSessionContext(currentStackFrame)
        )
    }
}
