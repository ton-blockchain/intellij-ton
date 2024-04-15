package org.ton.intellij.tact.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.ton.intellij.tact.psi.*
import org.ton.intellij.tact.stub.TactConstantStub
import org.ton.intellij.tact.stub.TactFieldStub
import org.ton.intellij.tact.stub.TactFunctionStub
import org.ton.intellij.tact.stub.TactTraitStub
import org.ton.intellij.tact.type.TactTy
import org.ton.intellij.tact.type.TactTyRef
import org.ton.intellij.util.getChildrenByType
import org.ton.intellij.util.recursionGuard

abstract class TactTraitImplMixin : TactNamedElementImpl<TactTraitStub>, TactTrait {
    constructor(node: ASTNode) : super(node)

    constructor(stub: TactTraitStub, type: IStubElementType<*, *>) : super(stub, type)

    override val declaredTy: TactTy
        get() = TactTyRef(this)

    override val superTraits: Sequence<TactTypeDeclarationElement>
        get() = recursionGuard(this, {
            sequence {
                withClause?.typeList?.forEach {
                    val typeDeclaration = (it.reference?.resolve() as? TactTypeDeclarationElement) ?: return@forEach
                    yield(typeDeclaration)
                    yieldAll(typeDeclaration.superTraits)
                }
            }
        }, memoize = false) ?: emptySequence()

    val constants: List<TactConstant>
        get() = CachedValuesManager.getCachedValue(this) {
            val stub = stub
            val functions = if (stub != null) {
                getChildrenByType(stub, TactElementTypes.CONSTANT, TactConstantStub.Type.ARRAY_FACTORY)
            } else {
                traitBody?.constantList ?: emptyList()
            }
            CachedValueProvider.Result.create(functions, this)
        }

    val fields: List<TactField>
        get() = CachedValuesManager.getCachedValue(this) {
            val stub = stub
            val functions = if (stub != null) {
                getChildrenByType(stub, TactElementTypes.FIELD, TactFieldStub.Type.ARRAY_FACTORY)
            } else {
                traitBody?.fieldList ?: emptyList()
            }
            CachedValueProvider.Result.create(functions, this)
        }

    val functions: List<TactFunction>
        get() = CachedValuesManager.getCachedValue(this) {
            val stub = stub
            val functions = if (stub != null) {
                getChildrenByType(stub, TactElementTypes.FUNCTION, TactFunctionStub.Type.ARRAY_FACTORY)
            } else {
                traitBody?.functionList ?: emptyList()
            }
            CachedValueProvider.Result.create(functions, this)
        }

    override val members: Sequence<TactNamedElement>
        get() = recursionGuard(this, {
            sequence {
                yieldAll(constants.asSequence())
                yieldAll(fields.asSequence())
                yieldAll(functions.asSequence())

                superTraits.forEach {
                    yieldAll(it.members)
                }
            }
        }, memoize = false) ?: emptySequence()
}
