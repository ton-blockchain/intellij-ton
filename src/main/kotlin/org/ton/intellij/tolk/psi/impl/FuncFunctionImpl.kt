package org.ton.intellij.tolk.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.util.PsiTreeUtil
import org.ton.intellij.tolk.TolkIcons
import org.ton.intellij.tolk.psi.TolkElementTypes
import org.ton.intellij.tolk.psi.TolkFunction
import org.ton.intellij.tolk.stub.TolkFunctionStub
import org.ton.intellij.tolk.type.ty.TolkTy
import org.ton.intellij.tolk.type.ty.TolkTyMap
import org.ton.intellij.tolk.type.ty.TolkTyUnknown
import org.ton.intellij.tolk.type.ty.rawType
import javax.swing.Icon

abstract class TolkFunctionMixin : TolkNamedElementImpl<TolkFunctionStub>, TolkFunction {
    constructor(node: ASTNode) : super(node)

    constructor(stub: TolkFunctionStub, stubType: IStubElementType<*, *>) : super(stub, stubType)

    override fun getIcon(flags: Int): Icon? {
        return TolkIcons.FUNCTION
    }

    override fun toString(): String = "TolkFunction($containingFile - $name)"
}

val TolkFunction.isMutable: Boolean
    get() = stub?.isMutable ?: (node.findChildByType(TolkElementTypes.TILDE) != null)

val TolkFunction.isDeprecated: Boolean
    get() = stub?.isDeprecated ?: functionAnnotationList.any { it.textMatches("@deprecated") }

val TolkFunction.hasMethodId: Boolean
    get() = stub?.hasMethodId ?: methodIdDefinitionList.isNotEmpty()

val TolkFunction.hasAsm: Boolean
    get() = stub?.hasAsm ?: (asmDefinition != null)

val TolkFunction.isBuiltin: Boolean
    get() = stub?.isBuiltin ?: (builtinKeyword != null)

val TolkFunction.rawReturnType: TolkTy
    get() = typeReference?.rawType ?: TolkTyUnknown

val TolkFunction.rawParamType: TolkTy
    get() = TolkTy(functionParameterList.map {
        it.typeReference?.rawType ?: TolkTyUnknown
    })

val TolkFunction.rawType: TolkTyMap
    get() = TolkTyMap(
        rawParamType,
        rawReturnType
    )
