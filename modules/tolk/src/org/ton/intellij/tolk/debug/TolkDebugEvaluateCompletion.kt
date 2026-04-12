package org.ton.intellij.tolk.debug

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.patterns.ElementPattern
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import com.intellij.xdebugger.XDebuggerManager
import org.ton.intellij.tolk.TolkIcons
import org.ton.intellij.tolk.psi.TolkReferenceExpression
import org.ton.intellij.util.psiElement

internal object TolkDebugEvaluateCompletion {
    private val CONTEXT_KEY = Key.create<Context>("org.ton.intellij.tolk.debug.evaluate.completion")

    fun install(document: Document, context: Context) {
        document.putUserData(CONTEXT_KEY, context)
    }

    fun context(document: Document): Context? {
        return document.getUserData(CONTEXT_KEY)
    }

    interface Context {
        fun completionItems(project: Project): List<Item>
    }

    data class Item(
        val lookupString: String,
        val presentableText: String,
        val typeText: String?,
        val scopeName: String?,
    )
}

internal class TolkDebugEvaluateSessionContext(
    private val capturedStackFrame: TolkDapXStackFrame?
) : TolkDebugEvaluateCompletion.Context {
    override fun completionItems(project: Project): List<TolkDebugEvaluateCompletion.Item> {
        val currentStackFrame = XDebuggerManager.getInstance(project).currentSession?.currentStackFrame as? TolkDapXStackFrame
        return (currentStackFrame ?: capturedStackFrame)?.completionItems.orEmpty()
    }
}

internal object TolkDebugEvaluateCompletionProvider : CompletionProvider<CompletionParameters>() {
    private const val DEBUG_SCOPE_PRIORITY = 100.0

    val elementPattern: ElementPattern<out PsiElement> =
        psiElement<PsiElement>().withParent(psiElement<TolkReferenceExpression>())

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val completionContext = TolkDebugEvaluateCompletion.context(parameters.editor.document) ?: return
        val reference = parameters.position.parent as? TolkReferenceExpression ?: return
        if (parameters.position != reference.identifier) return

        completionContext.completionItems(parameters.position.project).forEach { item ->
            val builder = LookupElementBuilder.create(item.lookupString)
                .withPresentableText(item.presentableText)
                .withTypeText(item.typeText)
                .withIcon(TolkIcons.VARIABLE)
                .let { lookup ->
                    val scopeName = item.scopeName
                    if (scopeName.isNullOrBlank()) lookup else lookup.withTailText(" ($scopeName)", true)
                }
            result.addElement(PrioritizedLookupElement.withPriority(builder, DEBUG_SCOPE_PRIORITY))
        }
    }
}
