package org.ton.intellij.tolk.ide.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.template.TemplateManager
import com.intellij.icons.AllIcons
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.PsiElement
import com.intellij.psi.util.parentOfType
import com.intellij.util.ProcessingContext
import org.ton.intellij.tolk.psi.TolkMatchExpression
import org.ton.intellij.tolk.psi.TolkMatchPattern
import org.ton.intellij.tolk.psi.TolkReferenceExpression
import org.ton.intellij.tolk.type.*

object TolkAllMatchTypesCompletionProvider : TolkCompletionProvider() {
    override val elementPattern: ElementPattern<out PsiElement> =
        psiElement()
            .withParent(TolkReferenceExpression::class.java)
            .withSuperParent(2, TolkMatchPattern::class.java)

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        val position = parameters.position
        val matchExpr = position.parentOfType<TolkMatchExpression>() ?: return
        if (!isMatchEmpty(matchExpr)) return

        val expr = matchExpr.expression ?: return

        val type = expr.type ?: TolkTy.Unknown
        val unwrappedType = type.unwrapTypeAlias()
        if (unwrappedType is TolkTyUnion) {
            val element =
                LookupElementBuilder.create("")
                    .withPresentableText("Fill all cases…")
                    .withIcon(AllIcons.Actions.RealIntentionBulb)
                    .withInsertHandler(MatchTypesInsertHandler(unwrappedType.variants))

            result.addElement(element)
        }
    }

    private fun isMatchEmpty(matchExpr: TolkMatchExpression): Boolean {
        if (matchExpr.matchArmList.isEmpty()) {
            return true
        }

        if (matchExpr.matchArmList.size > 1) {
            return false
        }

        // match (expr) {
        //     IntellijIdeaRulezzzz<caret>
        //     ^^^^^^^^^^^^^^^^^^^^ treat as a match arm, so check for body
        // }
        val firstArm = matchExpr.matchArmList.first()
        return firstArm.matchBody == null
    }

    class MatchTypesInsertHandler(
        private val types: Collection<TolkTy>,
    ) : InsertHandler<LookupElement> {
        override fun handleInsert(context: InsertionContext, item: LookupElement) {
            val project = context.project

            val patterns = types.map { it.render() } + "else"
            val arms = patterns.mapIndexed { index, pattern -> "$pattern => {\n${if (index == 0) "\$END$" else ""}\n}" }
            val templateText = arms.joinToString("\n")

            val template = TemplateManager.getInstance(project).createTemplate("match-types", "tolk", templateText)
            template.isToReformat = true
            TemplateManager.getInstance(project).startTemplate(context.editor, template)
        }
    }
}
