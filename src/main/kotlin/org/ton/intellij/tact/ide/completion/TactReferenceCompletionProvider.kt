package org.ton.intellij.tact.ide.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.util.ProcessingContext
import org.ton.intellij.tact.psi.TactInferenceContextOwner
import org.ton.intellij.tact.psi.TactReferenceExpression
import org.ton.intellij.tact.psi.impl.isGet
import org.ton.intellij.tact.stub.index.TactFunctionIndex
import org.ton.intellij.tact.type.TactLookup
import org.ton.intellij.tact.type.collectVariableCandidates
import org.ton.intellij.tact.type.ty
import org.ton.intellij.util.ancestorStrict
import org.ton.intellij.util.processAllKeys

class TactReferenceCompletionProvider : TactCompletionProvider() {
    override val elementPattern = psiElement().withParent(TactReferenceExpression::class.java)

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val position = parameters.position.parent as? TactReferenceExpression ?: return
        val inferenceContextOwner = position.ancestorStrict<TactInferenceContextOwner>() ?: return
        val project = position.project
        val lookup = TactLookup(project, inferenceContextOwner)

        collectVariableCandidates(position).distinctBy { it.name }.forEach {
            result.addElement(LookupElementBuilder.createWithIcon(it))
        }

        processAllKeys(TactFunctionIndex.KEY, project) { key ->
            TactFunctionIndex.findElementsByName(project, key).asSequence()
                .filter { !it.isGet }
                .distinctBy { it.name }
                .forEach { function ->
                    result.addElement(
                        LookupElementBuilder
                            .createWithIcon(function)
                            .withTypeText(function.type?.ty?.toString() ?: "")
                    )
                }
            true
        }
    }
}
