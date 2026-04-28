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
import org.ton.intellij.tolk.psi.TolkParameterElement
import org.ton.intellij.tolk.psi.TolkReferenceElement
import org.ton.intellij.tolk.psi.TolkReferenceExpression
import org.ton.intellij.tolk.psi.TolkSelfParameter
import org.ton.intellij.tolk.psi.TolkVar

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
    val resolved = findEvaluatableElementAtOffset(file, document, offset) ?: return null
    if (resolved.expression?.isCallCallee() == true) return null

    return ExpressionInfo(resolved.textRange, resolved.expressionText, resolved.expressionText)
}

private fun findEvaluatableElementAtOffset(file: TolkFile, document: Document, offset: Int): TolkEvaluatableElement? {
    val normalizedOffset = offset.coerceIn(0, document.textLength)
    val candidateOffsets = listOf(normalizedOffset, normalizedOffset - 1).distinct().filter { it >= 0 }
    for (candidateOffset in candidateOffsets) {
        val leaf = file.findElementAt(candidateOffset) ?: continue
        val declaration = leaf.findDeclarationElement()
        if (declaration != null) {
            return declaration
        }

        val dotExpression = leaf.findFieldAccessExpression()
        if (dotExpression != null) {
            return TolkEvaluatableElement(dotExpression, dotExpression.textRange, dotExpression.text)
        }

        val referenceExpression = PsiTreeUtil.getParentOfType(leaf, TolkReferenceExpression::class.java, false)
        if (referenceExpression != null && referenceExpression.containsReferenceName(leaf)) {
            return TolkEvaluatableElement(
                expression = referenceExpression,
                textRange = referenceExpression.textRange,
                expressionText = referenceExpression.text,
            )
        }
    }

    return null
}

private fun PsiElement.findDeclarationElement(): TolkEvaluatableElement? {
    val variable = PsiTreeUtil.getParentOfType(this, TolkVar::class.java, false)
    if (variable != null && variable.identifier.textRange.containsOffset(textRange.startOffset)) {
        val variableName = variable.name ?: return null
        return TolkEvaluatableElement(
            expression = null,
            textRange = variable.identifier.textRange,
            expressionText = variableName,
        )
    }

    val parameter = PsiTreeUtil.getParentOfType(this, TolkParameterElement::class.java, false)
    if (parameter != null) {
        val identifier = parameter.identifier
        if (identifier != null && identifier.textRange.containsOffset(textRange.startOffset)) {
            val parameterName = parameter.name ?: return null
            return TolkEvaluatableElement(
                expression = null,
                textRange = identifier.textRange,
                expressionText = parameterName,
            )
        }
    }

    val selfParameter = PsiTreeUtil.getParentOfType(this, TolkSelfParameter::class.java, false)
    if (selfParameter != null) {
        val identifier = selfParameter.identifier
        if (identifier != null && identifier.textRange.containsOffset(textRange.startOffset)) {
            val selfParameterName = selfParameter.name ?: return null
            return TolkEvaluatableElement(
                expression = null,
                textRange = identifier.textRange,
                expressionText = selfParameterName,
            )
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

private data class TolkEvaluatableElement(
    val expression: TolkExpression?,
    val textRange: TextRange,
    val expressionText: String,
)
