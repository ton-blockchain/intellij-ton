package org.ton.intellij.tolk.refactor

import com.intellij.codeInsight.PsiEquivalenceUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.util.Conditions
import com.intellij.openapi.util.Pass
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.parentOfType
import com.intellij.refactoring.IntroduceTargetChooser
import com.intellij.refactoring.RefactoringBundle
import com.intellij.refactoring.introduce.inplace.InplaceVariableIntroducer
import com.intellij.refactoring.introduce.inplace.OccurrencesChooser
import com.intellij.refactoring.util.CommonRefactoringUtil
import com.intellij.util.ArrayUtil
import org.ton.intellij.tolk.psi.TolkBlockStatement
import org.ton.intellij.tolk.psi.TolkCallExpression
import org.ton.intellij.tolk.psi.TolkExpression
import org.ton.intellij.tolk.psi.TolkFile
import org.ton.intellij.tolk.psi.TolkParenExpression
import org.ton.intellij.tolk.psi.TolkPsiFactory
import org.ton.intellij.tolk.psi.TolkReferenceExpression
import org.ton.intellij.tolk.psi.TolkStatement
import org.ton.intellij.tolk.psi.TolkVar
import org.ton.intellij.tolk.psi.unwrapParentheses

open class TolkIntroduceVariableBase {
    fun performAction(operation: TolkIntroduceOperation) {
        val selectionModel = operation.editor.selectionModel
        val hasSelection = selectionModel.hasSelection()

        var expression = if (hasSelection) {
            findExpressionInSelection(operation.file, selectionModel.selectionStart, selectionModel.selectionEnd)
        } else {
            findExpressionAtOffset(operation)
        }

        expression = expression?.unwrapParentheses()

        if (expression == null) {
            val message =
                RefactoringBundle.message(if (hasSelection) "selected.block.should.represent.an.expression" else "refactoring.introduce.selection.error")
            showCannotPerform(operation, message)
            return
        }

        val extractableExpressions = collectExtractableExpressions(expression)
        if (extractableExpressions.isEmpty()) {
            showCannotPerform(operation, RefactoringBundle.message("refactoring.introduce.context.error"))
            return
        }

        if (extractableExpressions.size == 1 || hasSelection || ApplicationManager.getApplication().isUnitTestMode) {
            operation.expression = extractableExpressions.first()
            performOnElement(operation)
            return
        }

        IntroduceTargetChooser.showChooser(operation.editor, extractableExpressions, object : Pass<TolkExpression>() {
            override fun pass(expression: TolkExpression) {
                if (!expression.isValid) return
                operation.expression = expression
                performOnElement(operation)
            }
        }) {
            if (it.isValid) it.text
            else "<invalid expression>"
        }
    }

    private fun findExpressionInSelection(file: PsiFile, start: Int, end: Int): TolkExpression? {
        return PsiTreeUtil.findElementOfClassAtRange(file, start, end, TolkExpression::class.java)
    }

    private fun findExpressionAtOffset(operation: TolkIntroduceOperation): TolkExpression? {
        val file = operation.file
        val offset = operation.editor.caretModel.offset
        val expr = PsiTreeUtil.getNonStrictParentOfType(file.findElementAt(offset), TolkExpression::class.java)
        val preExpr = PsiTreeUtil.getNonStrictParentOfType(file.findElementAt(offset - 1), TolkExpression::class.java)
        return if (expr == null || preExpr != null && PsiTreeUtil.isAncestor(expr, preExpr, false)) preExpr else expr
    }

    private fun collectExtractableExpressions(expression: TolkExpression): List<TolkExpression> {
        if (expression.parentOfType<TolkStatement>() == null) {
            // not inside function
            return emptyList()
        }

        return SyntaxTraverser.psiApi().parents(expression).takeWhile(
            Conditions.notInstanceOf(
                TolkFile::class.java
            )
        )
            .filter(TolkExpression::class.java)
            .filter(Conditions.notInstanceOf(TolkParenExpression::class.java))
            .filter { it !is TolkReferenceExpression || it.parent !is TolkCallExpression }
            .toList()
    }

    private fun getLocalOccurrences(element: PsiElement): List<PsiElement?> {
        return getOccurrences(element, PsiTreeUtil.getTopmostParentOfType(element, TolkBlockStatement::class.java))
    }

    private fun getOccurrences(pattern: PsiElement, context: PsiElement?): List<PsiElement> {
        if (context == null) return emptyList()
        val occurrences = mutableListOf<PsiElement>()
        val visitor = object : PsiRecursiveElementVisitor() {
            override fun visitElement(element: PsiElement) {
                if (PsiEquivalenceUtil.areElementsEquivalent(element, pattern)) {
                    occurrences.add(element)
                    return
                }
                super.visitElement(element)
            }
        }
        context.acceptChildren(visitor)
        return occurrences
    }

    private fun performOnElement(operation: TolkIntroduceOperation) {
        val expression = operation.expression ?: return
        operation.occurrences = getLocalOccurrences(expression)
        val editor = operation.editor

        if (editor.settings.isVariableInplaceRenameEnabled) {
            operation.name = "name"
            if (ApplicationManager.getApplication().isUnitTestMode) {
                performInplaceIntroduce(operation)
                return
            }

            OccurrencesChooser.simpleChooser<PsiElement>(editor)
                .showChooser(expression, operation.occurrences, object : Pass<OccurrencesChooser.ReplaceChoice>() {
                    override fun pass(choice: OccurrencesChooser.ReplaceChoice) {
                        operation.replaceAll = choice == OccurrencesChooser.ReplaceChoice.ALL
                        performInplaceIntroduce(operation)
                    }
                })

            return
        }

        val dialog = TolkIntroduceVariableDialog(operation)
        if (dialog.showAndGet()) {
            operation.name = dialog.name
            performReplace(operation)
        }
    }

    private fun performInplaceIntroduce(operation: TolkIntroduceOperation) {
        if (!operation.expression!!.isValid) {
            showCannotPerform(operation, RefactoringBundle.message("refactoring.introduce.context.error"))
            return
        }
        performReplace(operation)
        TolkInplaceVariableIntroducer(operation).performInplaceRefactoring(null)
    }

    private fun performReplace(operation: TolkIntroduceOperation) {
        val project = operation.project
        val expression = operation.expression ?: return
        val occurrences = if (operation.replaceAll) operation.occurrences else listOf(expression)
        val anchor = findLocalAnchor(occurrences)
        if (anchor == null) {
            showCannotPerform(operation, RefactoringBundle.message("refactoring.introduce.context.error"))
            return
        }

        val context = anchor.parent as TolkBlockStatement
        val name = operation.name
        val newOccurrences = mutableListOf<PsiElement>()

        WriteCommandAction.runWriteCommandAction(project, "Introduce Variable", null, {
            val newVariableDecl = TolkPsiFactory[project].createVariableDeclaration(name, expression.text)
            val newLine = TolkPsiFactory[project].createNewline()

            val statement = context.addBefore(newVariableDecl, anchor)
            context.addAfter(newLine, statement)
            val varDefinition = PsiTreeUtil.findChildOfType(statement, TolkVar::class.java)!!

            assert(varDefinition.isValid) { "invalid var `" + varDefinition.text + "` definition in `" + statement.text + "`" }

            operation.variable = varDefinition

            for (occurrence in occurrences) {
                val occ = occurrence?.unwrapPsiParentheses() ?: continue
                newOccurrences.add(occ.replace(TolkPsiFactory[project].createIdentifier(name)))
            }
            operation.editor.caretModel.moveToOffset(varDefinition.identifier.textRange.startOffset)
        })

        operation.occurrences = newOccurrences
        PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(operation.editor.document)
    }

    private fun findLocalAnchor(occurrences: List<PsiElement?>): PsiElement? {
        return findAnchor(
            occurrences,
            PsiTreeUtil.getNonStrictParentOfType(PsiTreeUtil.findCommonParent(occurrences), TolkBlockStatement::class.java)
        )
    }

    private fun findAnchor(occurrences: List<PsiElement?>, context: PsiElement?): PsiElement? {
        val first = occurrences.firstOrNull()
        var statement: PsiElement? = PsiTreeUtil.getNonStrictParentOfType(first, TolkStatement::class.java)
        while (statement != null && statement.parent !== context) {
            statement = statement.parent
        }
        return statement
    }

    private fun showCannotPerform(operation: TolkIntroduceOperation, message: String) {
        val msg = RefactoringBundle.getCannotRefactorMessage(message)
        @Suppress("DialogTitleCapitalization")
        CommonRefactoringUtil.showErrorHint(
            operation.project, operation.editor, msg,
            RefactoringBundle.getCannotRefactorMessage(null), "refactoring.extractVariable"
        )
    }

    private class TolkInplaceVariableIntroducer(operation: TolkIntroduceOperation) :
        InplaceVariableIntroducer<PsiElement?>(
            operation.variable, operation.editor, operation.project, "Introduce Variable",
            ArrayUtil.toObjectArray(operation.occurrences, PsiElement::class.java), null
        )


    fun PsiElement.unwrapPsiParentheses(): PsiElement? {
        var current: PsiElement? = this
        while (current is TolkParenExpression) {
            current = current.expression
        }
        return current
    }
}
