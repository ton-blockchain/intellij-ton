package org.ton.intellij.tolk.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.ton.intellij.tolk.TolkIcons
import org.ton.intellij.tolk.psi.TolkElementTypes
import org.ton.intellij.tolk.psi.TolkFunction
import org.ton.intellij.tolk.stub.TolkFunctionStub
import org.ton.intellij.tolk.type.*
import javax.swing.Icon

abstract class TolkFunctionMixin : TolkNamedElementImpl<TolkFunctionStub>, TolkFunction {
    constructor(node: ASTNode) : super(node)

    constructor(stub: TolkFunctionStub, stubType: IStubElementType<*, *>) : super(stub, stubType)

    override fun getIcon(flags: Int): Icon? {
        return TolkIcons.FUNCTION
    }

    override fun toString(): String = "TolkFunction($containingFile - $name)"

    override fun getBaseIcon(): Icon? = TolkIcons.FUNCTION

    override val type: TolkType?
        get() = CachedValuesManager.getCachedValue(this) {
            val parameters = parameterList?.parameterList?.map { parameter ->
                parameter.typeExpression?.type ?: return@getCachedValue null.also {
                    println("[${containingFile.name}] Failed to get type for $parameter - `${parameter.text}`")
                }
            } ?: emptyList()
            val returnType = returnType ?: return@getCachedValue null
            val type = TolkFunctionType(TolkType.tensor(parameters), returnType)

            CachedValueProvider.Result.create(type, this)
        }

    private val returnType: TolkType?
        get() = CachedValuesManager.getCachedValue(this) {
            var typeExpressionType = typeExpression?.type
            if (typeExpressionType == TolkType.Unknown) {
                typeExpressionType = null
            }
            if (typeExpressionType != null) {
                return@getCachedValue CachedValueProvider.Result.create(
                    typeExpressionType,
                    this
                )
            }

            val inference = try {
                inference
            } catch (_: CyclicReferenceException) {
                null
            } ?: return@getCachedValue CachedValueProvider.Result.create(null, this)

            var result = if (inference.returnStatements.isNotEmpty()) {
                inference.returnStatements.asSequence().map {
                    it.expression?.type
                }.filterNotNull().fold(null) { a, b -> a?.join(b) ?: b }
            } else if (inference.unreachable == TolkUnreachableKind.ThrowStatement) {
                TolkType.Never
            } else {
                TolkType.Unit
            }
            if (result == TolkType.Unknown) {
                result = null
            }

           CachedValueProvider.Result.create(result, this)
        }
}

val TolkFunction.isMutable: Boolean
    get() = stub?.isMutable ?: (node.findChildByType(TolkElementTypes.TILDE) != null)

val TolkFunction.isDeprecated: Boolean
    get() = stub?.isDeprecated ?: annotationList.any { it.identifier?.textMatches("deprecated") == true }

val TolkFunction.getKeyword get() = node.findChildByType(TolkElementTypes.GET_KEYWORD)

val TolkFunction.isGetMethod: Boolean
    get() = stub?.isGetMethod
        ?: (getKeyword != null || annotationList.any { it.identifier?.textMatches("method_id") == true })

val TolkFunction.hasAsm: Boolean
    get() = stub?.hasAsm ?: (functionBody?.asmDefinition != null)

val TolkFunction.isBuiltin: Boolean
    get() = stub?.isBuiltin ?: (functionBody?.builtinKeyword != null)
