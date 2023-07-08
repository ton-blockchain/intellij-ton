package org.ton.intellij.func.psi

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.util.ArrayFactory
import org.ton.intellij.func.FuncFileType
import org.ton.intellij.func.FuncLanguage
import org.ton.intellij.func.stub.FuncFileStub
import org.ton.intellij.func.stub.type.FuncIncludeDefinitionStubElementType

class FuncFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, FuncLanguage) {
    override fun getFileType(): FileType = FuncFileType

    override fun getStub(): FuncFileStub? = super.getStub() as? FuncFileStub

    val includeDefinitions: List<FuncIncludeDefinition>
        get() = CachedValuesManager.getCachedValue(this) {
            val stub = stub
            val childrens = if (stub != null) getChildrenByType(
                stub,
                FuncElementTypes.INCLUDE_DEFINITION,
                FuncIncludeDefinitionStubElementType.ARRAY_FACTORY
            ) else calcIncludeDefinitions()
            CachedValueProvider.Result.create(childrens, this)
        }

    private fun calcIncludeDefinitions(): List<FuncIncludeDefinition> {
        println("Calculation include definitions...")
        return findChildrenByClass(FuncIncludeDefinitionList::class.java).flatMap {
            it.includeDefinitionList
        }
    }
}

private fun <E : PsiElement> getChildrenByType(
    stub: StubElement<out PsiElement>,
    elementType: IElementType,
    f: ArrayFactory<E?>,
): List<E> {
    return stub.getChildrenByType(elementType, f).toList() as List<E>
}
