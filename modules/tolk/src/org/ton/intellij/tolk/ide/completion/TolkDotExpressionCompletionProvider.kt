package org.ton.intellij.tolk.ide.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.StandardPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StubIndex
import com.intellij.psi.util.parentOfType
import com.intellij.util.ProcessingContext
import org.ton.intellij.tolk.psi.TolkDotExpression
import org.ton.intellij.tolk.psi.TolkFunction
import org.ton.intellij.tolk.psi.impl.toLookupElement
import org.ton.intellij.tolk.stub.index.TolkFunctionIndex
import org.ton.intellij.tolk.type.TyStruct
import org.ton.intellij.tolk.type.render

object TolkDotExpressionCompletionProvider : TolkCompletionProvider() {
    override val elementPattern: ElementPattern<out PsiElement>
        get() = PlatformPatterns.psiElement()
            .afterLeaf(".")
            .withSuperParent(2, TolkDotExpression::class.java)
            .andNot(PlatformPatterns.psiElement().afterLeaf(
                PlatformPatterns.psiElement().withText(StandardPatterns.string().matches("\\d+"))
            ))


    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val project = parameters.originalFile.project
        val dotExpression = parameters.position.parentOfType<TolkDotExpression>() ?: return
        val leftType = dotExpression.left.type ?: return
        val actualLeftType = leftType.unwrapTypeAlias().actualType()
        if (actualLeftType is TyStruct) {
            actualLeftType.psi?.structBody?.structFieldList?.forEach { field ->
                result.addElement(
                    LookupElementBuilder
                        .createWithIcon(field)
                        .apply {
                            field.type?.render()?.let {
                                withTypeText(it)
                            }
                        }
                )
            }
        }

        StubIndex.getInstance().processAllKeys(TolkFunctionIndex.KEY, project) { key ->
            StubIndex.getInstance().processElements(
                TolkFunctionIndex.KEY,
                key,
                project,
                GlobalSearchScope.allScope(project),
                TolkFunction::class.java
            ) { function ->
                val receiverType = function.functionReceiver?.typeExpression?.type?.unwrapTypeAlias()?.actualType() ?: return@processElements true
                if (receiverType == actualLeftType) {
                    result.addElement(function.toLookupElement())
                }
                true
            }
            true
        }
    }
}
