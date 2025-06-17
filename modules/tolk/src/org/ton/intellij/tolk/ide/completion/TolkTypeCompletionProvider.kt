package org.ton.intellij.tolk.ide.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.ElementPattern
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StubIndex
import com.intellij.util.ProcessingContext
import org.ton.intellij.tolk.psi.TolkReferenceTypeExpression
import org.ton.intellij.tolk.psi.TolkSymbolElement
import org.ton.intellij.tolk.psi.TolkTypeParameterListOwner
import org.ton.intellij.tolk.psi.TolkTypeSymbolElement
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
        TolkTy.Address,
    ).map { it.toString() } + listOf(
        "uint8",
        "uint16",
        "uint32",
        "uint64",
        "uint128",
        "uint256",
        "int",
        "int8",
        "int16",
        "int32",
        "int64",
        "int128",
        "int256",
        "bytes",
    )

    private val cachedPrimitiveElements = primitiveTypes.map {
        LookupElementBuilder.create(it).withBoldness(true)
    }
    private val cachedVarIntElements = listOf(
        TolkTy.VarInt16,
        TolkTy.VarInt32,
    )

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val project = parameters.originalFile.project
        val position = parameters.position
        val parameterListOwner = position.parentOfType<TolkTypeParameterListOwner>()
        val ctx = TolkCompletionContext(position.parent as? TolkSymbolElement)

        result.restartCompletionOnPrefixChange("cont")
        if (result.prefixMatcher.prefix == "cont") {
            result.addElement(TolkTy.Continuation.toLookupElement())
        }
        result.restartCompletionOnPrefixChange("var")
        if (result.prefixMatcher.prefix == "var") {
            cachedVarIntElements.forEach { lookup ->
                result.addElement(lookup.toLookupElement())
            }
        }

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
            result.addElement(typeDef.toLookupElementBuilder(ctx))
        }
    }
}
