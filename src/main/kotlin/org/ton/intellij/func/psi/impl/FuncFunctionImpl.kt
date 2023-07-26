package org.ton.intellij.func.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.ton.intellij.func.psi.FuncElementTypes
import org.ton.intellij.func.psi.FuncFunction
import org.ton.intellij.func.stub.FuncFunctionStub
import org.ton.intellij.func.type.FuncType
import org.ton.intellij.func.type.FuncUnresolvedTypeException
import org.ton.intellij.func.type.funcType

abstract class FuncFunctionMixin : FuncNamedElementImpl<FuncFunctionStub>, FuncFunction {
    constructor(node: ASTNode) : super(node)

    constructor(stub: FuncFunctionStub, stubType: IStubElementType<*, *>) : super(stub, stubType)

    override var funcType
        get() = CachedValuesManager.getCachedValue(this) {
            val typeVars = this.typeParameterList?.typeParameterList?.map {
                (it as FuncTypeParameterImpl).funcType
            }
            val returnType = typeExpression.funcType
            val paramType = functionParameterList?.funcType
                ?: throw FuncUnresolvedTypeException("Can't resolve type of function '$name' (incomplete parameters)")

            val functionType = FuncType.map(paramType, returnType)
            val result = if (!typeVars.isNullOrEmpty()) {
                FuncType.forall(typeVars, functionType)
            } else {
                functionType
            }
            CachedValueProvider.Result.create(result, this)
        }
        set(value) {
        }
}

val FuncFunction.isImpure: Boolean
    get() = stub?.isImpure ?: (node.findChildByType(FuncElementTypes.IMPURE_KEYWORD) != null)

val FuncFunction.isMutable: Boolean
    get() = stub?.isMutable ?: (node.findChildByType(FuncElementTypes.TILDE) != null)

val FuncFunction.hasMethodId: Boolean
    get() = stub?.hasMethodId ?: (methodIdDefinition != null)

val FuncFunction.hasAsm: Boolean
    get() = stub?.hasAsm ?: (asmDefinition != null)
