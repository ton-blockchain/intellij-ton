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
import org.ton.intellij.func.stub.type.FuncFunctionStubElementType
import org.ton.intellij.func.stub.type.FuncIncludeDefinitionStubElementType

class FuncFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, FuncLanguage) {
    override fun getFileType(): FileType = FuncFileType

    override fun getStub(): FuncFileStub? = super.getStub() as? FuncFileStub

    val includeDefinitions: List<FuncIncludeDefinition>
        get() = CachedValuesManager.getCachedValue(this) {
            val stub = stub
            val children = if (stub != null) getChildrenByType(
                stub,
                FuncElementTypes.INCLUDE_DEFINITION,
                FuncIncludeDefinitionStubElementType.ARRAY_FACTORY
            ) else {
                findChildrenByClass(FuncIncludeDefinition::class.java).toList()
            }
            CachedValueProvider.Result.create(children, this)
        }

    val functions: List<FuncFunction>
        get() = CachedValuesManager.getCachedValue(this) {
            val stub = stub
            val functions = if (stub != null) {
                getChildrenByType(stub, FuncElementTypes.FUNCTION, FuncFunctionStubElementType.ARRAY_FACTORY)
            } else {
                findChildrenByClass(FuncFunction::class.java).toList()
            }
            CachedValueProvider.Result.create(functions, this)
        }

    override fun toString(): String = "FuncFile($name)"
}

private fun <E : PsiElement> getChildrenByType(
    stub: StubElement<out PsiElement>,
    elementType: IElementType,
    f: ArrayFactory<E?>,
): List<E> {
    return stub.getChildrenByType(elementType, f).toList() as List<E>
}
