package org.ton.intellij.tolk.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.ton.intellij.tolk.TolkIcons
import org.ton.intellij.tolk.psi.TolkElementTypes
import org.ton.intellij.tolk.psi.TolkFunction
import org.ton.intellij.tolk.stub.TolkFunctionStub
import org.ton.intellij.tolk.type.TolkType
import org.ton.intellij.tolk.type.TolkType.Function
import javax.swing.Icon

abstract class TolkFunctionMixin : TolkNamedElementImpl<TolkFunctionStub>, TolkFunction {
    constructor(node: ASTNode) : super(node)

    constructor(stub: TolkFunctionStub, stubType: IStubElementType<*, *>) : super(stub, stubType)

    override fun getIcon(flags: Int): Icon? {
        return TolkIcons.FUNCTION
    }

    override fun toString(): String = "TolkFunction($containingFile - $name)"

    override val type: TolkType?
        get() = CachedValuesManager.getCachedValue(this) {
            val parameters = parameterList?.parameterList?.map { parameter ->
                parameter.typeExpression?.type ?: return@getCachedValue null.also {
                    println("Failed to get type for $parameter")
                }
            } ?: emptyList()
            val returnType = typeExpression?.type ?: TolkType.Unit
            val type = Function(TolkType.create(parameters), returnType)

            CachedValueProvider.Result.create(type, this)
        }
}

val TolkFunction.isMutable: Boolean
    get() = stub?.isMutable ?: (node.findChildByType(TolkElementTypes.TILDE) != null)

val TolkFunction.isDeprecated: Boolean
    get() = stub?.isDeprecated ?: annotationList.any { it.identifier?.textMatches("deprecated") ?: false }

val TolkFunction.getKeyword get() = node.findChildByType(TolkElementTypes.GET_KEYWORD)

val TolkFunction.isGetMethod: Boolean
    get() = stub?.isGetMethod
        ?: (getKeyword != null || annotationList.any { it.identifier?.textMatches("method_id") ?: false })

val TolkFunction.hasAsm: Boolean
    get() = stub?.hasAsm ?: (functionBody?.asmDefinition != null)

val TolkFunction.isBuiltin: Boolean
    get() = stub?.isBuiltin ?: (functionBody?.builtinKeyword != null)

