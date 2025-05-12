package org.ton.intellij.tolk.ide.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.ElementPattern
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StubIndex
import com.intellij.util.ProcessingContext
import org.ton.intellij.tolk.psi.*
import org.ton.intellij.tolk.stub.index.TolkTypeSymbolIndex
import org.ton.intellij.tolk.type.TolkTy
import org.ton.intellij.util.parentOfType
import org.ton.intellij.util.psiElement

object TolkTypeCompletionProvider : TolkCompletionProvider() {
    override val elementPattern: ElementPattern<out PsiElement> =
        psiElement<PsiElement>().withParent(psiElement<TolkReferenceTypeExpression>())

    private val primitiveTypes = listOf(
        TolkTy.Bool,
        TolkTy.Null,
        TolkTy.Cell,
        TolkTy.Slice,
        TolkTy.Builder,
        TolkTy.Tuple,
        TolkTy.Never,
        TolkTy.Coins,
        TolkTy.VarInt16,
        TolkTy.VarInt32,
        TolkTy.Address,
    ).map { it.toString() } + listOf(
        "uint",
        "int",
        "bits",
        "bytes",
    )

    private val cachedPrimitiveElements = primitiveTypes.map {
        LookupElementBuilder.create(it).withBoldness(true)
    }

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val project = parameters.originalFile.project
        val position = parameters.position
        val parameterListOwner = position.parentOfType<TolkTypeParameterListOwner>()
        parameterListOwner?.typeParameterList?.typeParameterList?.forEach { type ->
            result.addElement(
                LookupElementBuilder.createWithIcon(type)
            )
        }
        cachedPrimitiveElements.forEach {
            result.addElement(it)
        }

        val typeCandidates = HashSet<TolkSymbolElement>()

        StubIndex.getInstance().processAllKeys(TolkTypeSymbolIndex.KEY, project) { key ->
            StubIndex.getInstance().processElements(
                TolkTypeSymbolIndex.KEY,
                key,
                project,
                GlobalSearchScope.allScope(project),
                TolkTypeSymbolElement::class.java
            ) {
                typeCandidates.add(it)
                true
            }
            true
        }

        typeCandidates.forEach { typeDef ->
            result.addElement(
                LookupElementBuilder.createWithIcon(typeDef)
                    .withInsertHandler { context, item ->
                        val file = item.psiElement?.containingFile
                        val insertFile = context.file as? TolkFile ?: return@withInsertHandler
                        val includeCandidateFile = file as? TolkFile ?: return@withInsertHandler
                        insertFile.import(includeCandidateFile)
                    }
            )
        }
    }
}
