package org.ton.intellij.tolk.ide.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.project.Project
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PatternCondition
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StubIndex
import com.intellij.psi.util.elementType
import com.intellij.util.ProcessingContext
import org.ton.intellij.tolk.TolkIcons
import org.ton.intellij.tolk.psi.*
import org.ton.intellij.tolk.psi.impl.hasReceiver
import org.ton.intellij.tolk.psi.impl.receiverTy
import org.ton.intellij.tolk.stub.index.TolkTypeSymbolIndex
import org.ton.intellij.tolk.type.TolkTyAlias
import org.ton.intellij.tolk.type.TolkTyUnknown
import org.ton.intellij.util.parentOfType

object TolkFunctionNameCompletionProvider : TolkCompletionProvider() {
    override val elementPattern: ElementPattern<out PsiElement> =
        PlatformPatterns.psiElement()
            .withSuperParent(2, TolkFile::class.java)
            .with(object : PatternCondition<PsiElement>("afterFunKeyword") {
                override fun accepts(t: PsiElement, context: ProcessingContext?): Boolean {
                    // accept
                    // fun <caret>
                    val afterFun = t.prevSibling?.prevSibling?.elementType == TolkElementTypes.FUN_KEYWORD
                    // and
                    // fun int.<caret>
                    val isFunName = (t.parent as? TolkFunction)?.nameIdentifier?.isEquivalentTo(t) == true
                    return afterFun || isFunName
                }
            })

    val functions: List<Pair<String, String>> = listOf(
        "onInternalMessage" to "in: InMessage",
        "onExternalMessage" to "inMsg: slice",
        "onBouncedMessage" to "in: InMessageBounced",
        "onRunTickTock" to "isTock: bool",
        "onSplitPrepare" to "",
        "onSplitInstall" to "",
        "main" to "",
    )

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        val prefix = result.prefixMatcher.prefix
        if (prefix.isNotEmpty() && prefix[0].isUpperCase()) {
            addTypeCompletions(parameters.originalFile.project, parameters.position, result)
        }

        val func = parameters.position.parentOfType<TolkFunction>() ?: return
        val file = parameters.originalFile as? TolkFile ?: return
        val definedFunctions = file.functions.associateBy { it.name ?: "" }

        val hasBodyAndParams = func.parameterList != null && func.functionBody != null

        val isMethod = func.hasReceiver
        if (isMethod) {
            val receiverTy = func.receiverTy
            if (receiverTy is TolkTyAlias && receiverTy.underlyingType !is TolkTyUnknown) {
                if (hasBodyAndParams) {
                    result.addElement(
                        LookupElementBuilder.create("unpackFromSlice")
                            .withIcon(TolkIcons.FUNCTION)
                            .withTailText("(mutate s: slice)")
                    )
                    result.addElement(
                        LookupElementBuilder.create("packToBuilder")
                            .withIcon(TolkIcons.FUNCTION)
                            .withTailText("(self, mutate b: builder)")
                    )
                } else {
                    result.addElement(
                        LookupElementBuilder.create("unpackFromSlice")
                            .withIcon(TolkIcons.FUNCTION)
                            .withTailText("(mutate s: slice)")
                            .withInsertHandler(TemplateStringInsertHandler("(mutate s: slice) {\n\$END$\n}"))
                    )
                    result.addElement(
                        LookupElementBuilder.create("packToBuilder")
                            .withIcon(TolkIcons.FUNCTION)
                            .withTailText("(self, mutate b: builder)")
                            .withInsertHandler(TemplateStringInsertHandler("(self, mutate b: builder) {\n\$END$\n}"))
                    )
                }
            }
        } else {
            for ((name, signature) in functions) {
                if (definedFunctions.containsKey(name)) continue

                if (hasBodyAndParams) {
                    result.addElement(
                        LookupElementBuilder.create(name)
                            .withIcon(TolkIcons.FUNCTION)
                            .withTailText("(${signature})")
                    )
                } else {
                    result.addElement(
                        LookupElementBuilder.create(name)
                            .withIcon(TolkIcons.FUNCTION)
                            .withTailText("(${signature})")
                            .withInsertHandler(TemplateStringInsertHandler("(${signature}) {\n\$END$\n}"))
                    )
                }
            }
        }
    }

    private fun addTypeCompletions(project: Project, position: PsiElement, result: CompletionResultSet) {
        val ctx = TolkCompletionContext(position.parent as? TolkElement)
        val candidates = HashSet<TolkTypeSymbolElement>()
        val allKeys = mutableListOf<String>()

        StubIndex.getInstance().processAllKeys(TolkTypeSymbolIndex.KEY, project) { key ->
            allKeys.add(key)
            true
        }

        for (key in allKeys) {
            StubIndex.getInstance().processElements(
                TolkTypeSymbolIndex.KEY,
                key,
                project,
                GlobalSearchScope.allScope(project),
                TolkTypeSymbolElement::class.java
            ) {
                candidates.add(it)
                true
            }
        }

        for (type in candidates) {
            val lookup = type.toLookupElementBuilder(ctx)
            result.addElement(lookup)
        }
    }
}
