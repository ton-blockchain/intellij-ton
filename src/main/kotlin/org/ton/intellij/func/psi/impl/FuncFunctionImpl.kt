package org.ton.intellij.func.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType
import org.ton.intellij.func.FuncIcons
import org.ton.intellij.func.psi.FuncElementTypes
import org.ton.intellij.func.psi.FuncFunction
import org.ton.intellij.func.stub.FuncFunctionStub
import org.ton.intellij.func.type.ty.FuncTy
import org.ton.intellij.func.type.ty.FuncTyMap
import org.ton.intellij.func.type.ty.FuncTyUnknown
import org.ton.intellij.func.type.ty.rawType
import javax.swing.Icon

abstract class FuncFunctionMixin : FuncNamedElementImpl<FuncFunctionStub>, FuncFunction {
    constructor(node: ASTNode) : super(node)

    constructor(stub: FuncFunctionStub, stubType: IStubElementType<*, *>) : super(stub, stubType)

    override fun getIcon(flags: Int): Icon? {
        return FuncIcons.FUNCTION
    }

    override fun toString(): String = "FuncFunction($containingFile - $name)"
}

val FuncFunction.isImpure: Boolean
    get() = stub?.isImpure ?: (node.findChildByType(FuncElementTypes.IMPURE_KEYWORD) != null)

val FuncFunction.hasPure: Boolean
    get() = stub?.hasPure ?: (node.findChildByType(FuncElementTypes.PURE_KEYWORD) != null)

val FuncFunction.hasGet: Boolean
    get() = stub?.hasGet ?: (node.findChildByType(FuncElementTypes.GET_KEYWORD) != null)

val FuncFunction.isMutable: Boolean
    get() = stub?.isMutable ?: (node.findChildByType(FuncElementTypes.TILDE) != null)

val FuncFunction.hasMethodId: Boolean
    get() = stub?.hasMethodId ?: (methodIdDefinition != null)

val FuncFunction.hasAsm: Boolean
    get() = stub?.hasAsm ?: (asmDefinition != null)

val FuncFunction.rawReturnType: FuncTy
    get() = typeReference.rawType

val FuncFunction.rawParamType: FuncTy
    get() = FuncTy(functionParameterList.map {
        it.typeReference?.rawType ?: FuncTyUnknown
    })

val FuncFunction.rawType: FuncTyMap
    get() = FuncTyMap(
        rawParamType,
        rawReturnType
    )
