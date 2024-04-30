package org.ton.intellij.tact.ide.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.ElementPattern
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import org.ton.intellij.tact.psi.*
import org.ton.intellij.tact.stub.index.TactFunctionIndex
import org.ton.intellij.tact.type.TactTyRef
import org.ton.intellij.tact.type.selfInferenceResult
import org.ton.intellij.tact.type.selfType
import org.ton.intellij.tact.type.ty
import org.ton.intellij.util.ancestorStrict
import org.ton.intellij.util.processAllKeys
import org.ton.intellij.util.psiElement

class TactDotCompletionProvider : TactCompletionProvider() {
    override val elementPattern: ElementPattern<out PsiElement> =
        psiElement<PsiElement>().withSuperParent(2, TactDotExpression::class.java)

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val project = parameters.position.project
        val right = parameters.position.parent
        if (right !is TactFieldExpression && right !is TactCallExpression) return
        val dotExpression = right.parent as? TactDotExpression ?: return
        val left = dotExpression.expressionList.getOrNull(0) ?: return
        val inference = dotExpression.ancestorStrict<TactInferenceContextOwner>()?.selfInferenceResult ?: return

        val leftTy = inference.getExprTy(left)
        if (leftTy is TactTyRef) {
            leftTy.item.members.forEach { member ->
                var builder = LookupElementBuilder.createWithIcon(member)
                val typeText = when (member) {
                    is TactConstant -> member.type?.ty.toString()
                    is TactFunction -> member.type?.ty.toString()
                    else -> null
                }
                if (typeText != null) {
                    builder = builder.withTypeText(typeText)
                }
                builder.withInsertHandler { context, item ->
                    if (member is TactFunction) {
                        context.editor.document.insertString(context.editor.caretModel.offset, "()")
                        context.editor.caretModel.moveToOffset(context.editor.caretModel.offset + 2)
                        context.commitDocument()
                    }
                }
                result.addElement(builder)
            }
        }
        processAllKeys(TactFunctionIndex.KEY, project) { name ->
            val function = TactFunctionIndex.findElementsByName(project, name).find {
                it.selfType?.isAssignable(leftTy) == true
            }
            if (function != null) {
                result.addElement(
                    LookupElementBuilder.createWithIcon(function)
                        .withTypeText(function.type?.ty?.toString() ?: "")
                        .withInsertHandler { context, item ->
                            context.editor.document.insertString(context.editor.caretModel.offset, "()")
                            context.editor.caretModel.moveToOffset(context.editor.caretModel.offset + 2)
                            context.commitDocument()
                        }
                )
            }

            true
        }
    }
}
