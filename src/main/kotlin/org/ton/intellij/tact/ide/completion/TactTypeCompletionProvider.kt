package org.ton.intellij.tact.ide.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.util.ProcessingContext
import org.ton.intellij.tact.psi.TactReferencedType
import org.ton.intellij.tact.stub.index.TactTypesIndex
import org.ton.intellij.util.processAllKeys

class TactTypeCompletionProvider : TactCompletionProvider() {
    override val elementPattern = psiElement().withParent(TactReferencedType::class.java)

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val position = parameters.position
        val project = position.project
        processAllKeys(TactTypesIndex.KEY, project) { key ->
            TactTypesIndex.findElementsByName(project, key).forEach {
                result.addElement(LookupElementBuilder.createWithIcon(it))
                return@forEach
            }
            Int
            true
        }
    }
}
