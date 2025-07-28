package org.ton.intellij.tolk.ide.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.project.DumbAware
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PatternCondition
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.util.ProcessingContext
import org.ton.intellij.tolk.psi.TolkFunction
import org.ton.intellij.tolk.psi.impl.isGetMethod

object TolkBuiltinCompletionProvider : TolkCompletionProvider(), DumbAware {
    override val elementPattern: ElementPattern<out PsiElement>
        get() = psiElement().with(object : PatternCondition<PsiElement>("inFunctionDeclaration") {
            override fun accepts(t: PsiElement, context: ProcessingContext?): Boolean {
                // foo bar()
                //     <cursor>
                val parent = t.parent
                val grand = parent.parent
                // since IDEA inserts `intellijIdeaRulezzz`, we get:
                // foo bar()
                //     intellijIdeaRulezzz
                //
                // and this code is invalid, so we need to match the error parent node
                // as well as the grand node, which should be a function declaration (except get methods)
                return parent is PsiErrorElement && grand is TolkFunction && !grand.isGetMethod
            }
        })

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        result.addElement(LookupElementBuilder.create("builtin"))
    }
}
