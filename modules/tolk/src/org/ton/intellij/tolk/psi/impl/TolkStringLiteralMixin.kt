package org.ton.intellij.tolk.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.LiteralTextEscaper
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.PsiReference
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistry
import com.intellij.psi.stubs.IStubElementType
import org.ton.intellij.tolk.psi.*
import org.ton.intellij.tolk.psi.reference.TolkLiteralFileReferenceSet
import org.ton.intellij.tolk.stub.TolkStringLiteralStub

abstract class TolkStringLiteralMixin : TolkStubbedElementImpl<TolkStringLiteralStub>, TolkStringLiteral,
    PsiLanguageInjectionHost {

    constructor(stub: TolkStringLiteralStub, nodeType: IStubElementType<*, *>) : super(stub, nodeType)
    constructor(node: ASTNode) : super(node)

    override fun getReferences(): Array<out PsiReference?> {
        if (parent is TolkIncludeDefinition) {
            val rawString = rawString ?: return PsiReference.EMPTY_ARRAY
            val contents = rawString.text
            val fileSet = TolkLiteralFileReferenceSet(contents, this, rawString.startOffsetInParent)
            return fileSet.allReferences
        }
        return ReferenceProvidersRegistry.getReferencesFromProviders(this)
    }

    override fun isValidHost(): Boolean {
        return node.findChildByType(TolkElementTypes.RAW_STRING) != null
    }

    override fun updateText(text: String): PsiLanguageInjectionHost? {
        val newStringLiteral = (TolkPsiFactory[project].createExpression(text) as TolkLiteralExpression).stringLiteral!!
        rawString?.replace(newStringLiteral.rawString!!)
        return this
    }

    override fun createLiteralTextEscaper(): LiteralTextEscaper<out PsiLanguageInjectionHost?> {
        return SimpleMultiLineTextEscaper(this)
    }

    private class SimpleMultiLineTextEscaper<T : PsiLanguageInjectionHost>(host: T) : LiteralTextEscaper<T>(host) {
        override fun decode(rangeInsideHost: TextRange, outChars: java.lang.StringBuilder): Boolean {
            outChars.append(rangeInsideHost.substring(myHost.text))
            return true
        }

        override fun getOffsetInHost(offsetInDecoded: Int, rangeInsideHost: TextRange): Int {
            return rangeInsideHost.startOffset + offsetInDecoded
        }

        override fun isOneLine(): Boolean = false
    }
}
