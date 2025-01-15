package org.ton.intellij.tact.psi

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.psi.FileViewProvider
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.ton.intellij.tact.TactFileType
import org.ton.intellij.tact.TactLanguage
import org.ton.intellij.tact.stub.TactConstantStub
import org.ton.intellij.tact.stub.TactFileStub
import org.ton.intellij.tact.stub.TactTraitStub
import org.ton.intellij.util.getChildrenByType

class TactFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, TactLanguage), TactElement {
    override fun getFileType(): TactFileType = TactFileType

    override fun getStub(): TactFileStub? = super.getStub() as? TactFileStub

    val constants: List<TactConstant>
        get() = CachedValuesManager.getCachedValue(this) {
            val stub = stub
            val constants = if (stub != null) {
                getChildrenByType(stub, TactElementTypes.CONSTANT, TactConstantStub.Type.ARRAY_FACTORY)
            } else {
                findChildrenByClass(TactConstant::class.java).toList()
            }
            CachedValueProvider.Result.create(constants, this)
        }

    val traits: List<TactTrait>
        get() = CachedValuesManager.getCachedValue(this) {
            val stub = stub
            val traits = if (stub != null) {
                getChildrenByType(stub, TactElementTypes.TRAIT, TactTraitStub.Type.ARRAY_FACTORY)
            } else {
                findChildrenByClass(TactTrait::class.java).toList()
            }
            CachedValueProvider.Result.create(traits, this)
        }
}
