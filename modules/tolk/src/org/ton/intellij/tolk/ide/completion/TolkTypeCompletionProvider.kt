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
import org.ton.intellij.tolk.type.TolkBitsNTy
import org.ton.intellij.tolk.type.TolkIntNTy
import org.ton.intellij.tolk.type.TolkTy
import org.ton.intellij.tolk.type.render
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
        TolkTy.String,
        TolkTy.Builder,
        TolkTy.Never,
        TolkTy.Coins,
        TolkTy.Address,
        TolkTy.AnyAddress,
        TolkTy.Int,
        TolkIntNTy.fromName("uint8")!!,
        TolkIntNTy.fromName("uint16")!!,
        TolkIntNTy.fromName("uint32")!!,
        TolkIntNTy.fromName("uint64")!!,
        TolkIntNTy.fromName("uint128")!!,
        TolkIntNTy.fromName("uint256")!!,
        TolkIntNTy.fromName("int8")!!,
        TolkIntNTy.fromName("int16")!!,
        TolkIntNTy.fromName("int32")!!,
        TolkIntNTy.fromName("int64")!!,
        TolkIntNTy.fromName("int128")!!,
        TolkIntNTy.fromName("int256")!!,
        TolkBitsNTy.fromName("bits8")!!,
        TolkBitsNTy.fromName("bits16")!!,
        TolkBitsNTy.fromName("bits32")!!,
        TolkBitsNTy.fromName("bits64")!!,
        TolkBitsNTy.fromName("bits128")!!,
        TolkBitsNTy.fromName("bits256")!!,
    )

    private val cachedPrimitiveElements = primitiveTypes.map {
        it to LookupElementBuilder.create(it.render()).withBoldness(true)
    }
    private val cachedVarIntElements = listOf(
        TolkTy.VarInt16,
        TolkTy.VarInt32,
        TolkTy.VarUInt16,
        TolkTy.VarUInt32,
    )

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val project = parameters.originalFile.project
        val position = parameters.position
        val parameterListOwner = position.parentOfType<TolkTypeParameterListOwner>()
        val ctx = TolkCompletionContext(position.parent as? TolkElement)

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
        cachedPrimitiveElements.forEach { (_, lookup) ->
            result.addElement(lookup)
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

        for (typeDef in typeCandidates) {
            if (typeDef is TolkTypeDef) {
                if (typeDef.builtinKeyword != null) {
                    continue
                }
            }

            result.addElement(typeDef.toLookupElementBuilder(ctx))
        }
    }
}
