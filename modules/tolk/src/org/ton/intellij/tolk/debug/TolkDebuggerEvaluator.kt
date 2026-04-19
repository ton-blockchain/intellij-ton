package org.ton.intellij.tolk.debug

import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.xdebugger.XExpression
import com.intellij.xdebugger.XSourcePosition
import com.intellij.xdebugger.evaluation.EvaluationMode
import com.intellij.xdebugger.evaluation.ExpressionInfo
import com.intellij.xdebugger.evaluation.XDebuggerEvaluator
import org.ton.intellij.tolk.psi.TolkCallExpression
import org.ton.intellij.tolk.psi.TolkDotExpression
import org.ton.intellij.tolk.psi.TolkExpression
import org.ton.intellij.tolk.psi.TolkFieldLookup
import org.ton.intellij.tolk.psi.TolkFile
import org.ton.intellij.tolk.psi.TolkReferenceElement
import org.ton.intellij.tolk.psi.TolkReferenceExpression

internal class TolkDebuggerEvaluator(private val delegate: XDebuggerEvaluator) : XDebuggerEvaluator() {
    override fun evaluate(expression: String, callback: XEvaluationCallback, expressionPosition: XSourcePosition?) {
        delegate.evaluate(expression, callback, expressionPosition)
    }

    override fun evaluate(
        expression: XExpression,
        callback: XEvaluationCallback,
        expressionPosition: XSourcePosition?,
    ) {
        delegate.evaluate(expression, callback, expressionPosition)
    }

    override fun isCodeFragmentEvaluationSupported(): Boolean = delegate.isCodeFragmentEvaluationSupported

    override fun getExpressionRangeAtOffset(
        project: Project,
        document: Document,
        offset: Int,
        sideEffectsAllowed: Boolean,
    ): TextRange? {
        val file = getTolkFile(project, document) ?: return delegate.getExpressionRangeAtOffset(
            project,
            document,
            offset,
            sideEffectsAllowed,
        )
        return findExpressionInfoAtOffset(file, document, offset)?.textRange
    }

    override fun getExpressionInfoAtOffset(
        project: Project,
        document: Document,
        offset: Int,
        sideEffectsAllowed: Boolean,
    ): ExpressionInfo? {
        val file = getTolkFile(project, document) ?: return delegate.getExpressionInfoAtOffset(
            project,
            document,
            offset,
            sideEffectsAllowed,
        )
        return findExpressionInfoAtOffset(file, document, offset)
    }

    override fun formatTextForEvaluation(text: String): String = delegate.formatTextForEvaluation(text)

    override fun getEvaluationMode(text: String, startOffset: Int, endOffset: Int, psiFile: PsiFile?): EvaluationMode =
        delegate.getEvaluationMode(text, startOffset, endOffset, psiFile)
}

internal fun findExpressionInfoAtOffset(project: Project, document: Document, offset: Int): ExpressionInfo? {
    val file = getTolkFile(project, document) ?: return null
    return findExpressionInfoAtOffset(file, document, offset)
}

private fun getTolkFile(project: Project, document: Document): TolkFile? {
    val documentManager = PsiDocumentManager.getInstance(project)
    if (!documentManager.isCommitted(document)) return null
    return documentManager.getCachedPsiFile(document) as? TolkFile ?: documentManager.getPsiFile(document) as? TolkFile
}

private fun findExpressionInfoAtOffset(file: TolkFile, document: Document, offset: Int): ExpressionInfo? {
    val expression = findExpressionAtOffset(file, document, offset) ?: return null
    if (expression.isCallCallee()) return null

    val expressionText = expression.text
    return ExpressionInfo(expression.textRange, expressionText, expressionText)
}

private fun findExpressionAtOffset(file: TolkFile, document: Document, offset: Int): TolkExpression? {
    val normalizedOffset = offset.coerceIn(0, document.textLength)
    val candidateOffsets = listOf(normalizedOffset, normalizedOffset - 1).distinct().filter { it >= 0 }
    for (candidateOffset in candidateOffsets) {
        val leaf = file.findElementAt(candidateOffset) ?: continue
        val dotExpression = leaf.findFieldAccessExpression()
        if (dotExpression != null) {
            return dotExpression
        }

        val referenceExpression = PsiTreeUtil.getParentOfType(leaf, TolkReferenceExpression::class.java, false)
        if (referenceExpression != null && referenceExpression.containsReferenceName(leaf)) {
            return referenceExpression
        }
    }

    return null
}

private fun PsiElement.findFieldAccessExpression(): TolkDotExpression? {
    val fieldLookup = PsiTreeUtil.getParentOfType(this, TolkFieldLookup::class.java, false) ?: return null
    if (!fieldLookup.containsReferenceName(this)) return null
    return fieldLookup.parent as? TolkDotExpression
}

private fun TolkReferenceElement.containsReferenceName(element: PsiElement): Boolean {
    val referenceNameElement = referenceNameElement ?: return false
    return referenceNameElement.textRange.containsOffset(element.textRange.startOffset)
}

private fun TolkExpression.isCallCallee(): Boolean {
    val callExpression = parent as? TolkCallExpression ?: return false
    return callExpression.expression == this
}
