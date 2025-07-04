package org.ton.intellij.tolk.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.SimpleModificationTracker
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.isAncestor
import com.intellij.util.SmartList
import org.ton.intellij.tolk.TolkIcons
import org.ton.intellij.tolk.doc.psi.TolkDocComment
import org.ton.intellij.tolk.psi.*
import org.ton.intellij.tolk.stub.TolkFunctionStub
import org.ton.intellij.tolk.type.CyclicReferenceException
import org.ton.intellij.tolk.type.TolkTy
import org.ton.intellij.tolk.type.TolkTyFunction
import org.ton.intellij.tolk.type.inference
import org.ton.intellij.util.childOfType
import org.ton.intellij.util.greenStub
import javax.swing.Icon

abstract class TolkFunctionMixin : TolkNamedElementImpl<TolkFunctionStub>, TolkFunction {
    constructor(node: ASTNode) : super(node)

    constructor(stub: TolkFunctionStub, stubType: IStubElementType<*, *>) : super(stub, stubType)

    override val doc: TolkDocComment? get() = childOfType()

    override fun getIcon(flags: Int): Icon? {
        if (hasSelf) {
            return TolkIcons.METHOD
        }
        return TolkIcons.FUNCTION
    }

    override val type: TolkTyFunction
        get() = CachedValuesManager.getCachedValue(this, FUNCTION_TYPE) {
            val returnTy = returnTy
            val parameterList = parameterList ?: return@getCachedValue CachedValueProvider.Result.create(
                TolkTyFunction(
                    emptyList(),
                    returnTy
                ), this
            )
            val selfParameter = parameterList.selfParameter
            val parameters = parameterList.parameterList
            val parametersType: ArrayList<TolkTy>
            if (selfParameter != null) {
                parametersType = ArrayList(parameters.size + 1)
                parametersType.add(selfParameter.type ?: TolkTy.Unknown)
            } else {
                parametersType = ArrayList(parameters.size)
            }
            parameters.forEach {
                val type = it.typeExpression.type ?: TolkTy.Unknown
                parametersType.add(type)
            }

            val type = TolkTyFunction(parametersType, returnTy)

            createCachedResult(type)
        }

    val returnTy: TolkTy
        get() = CachedValuesManager.getCachedValue(this, RETURN_TYPE_KEY) {
            val returnType = resolveReturnType()
            createCachedResult(returnType)
        }

    val receiverTy: TolkTy
        get() = CachedValuesManager.getCachedValue(this, RECEIVER_TYPE_KEY) {
            val receiverType = functionReceiver?.typeExpression?.type ?: TolkTy.Unknown
            createCachedResult(receiverType)
        }

    override val modificationTracker = SimpleModificationTracker()

    override fun incModificationCount(element: PsiElement): Boolean {
        val blockStatement = functionBody?.blockStatement
        val shouldInc = blockStatement?.isAncestor(element) == true

        if (shouldInc) {
            if (returnType == null) {
                var hasReturn = false
                blockStatement.accept(object : TolkRecursiveElementWalkingVisitor() {
                    override fun visitExpression(o: TolkExpression) {}
                    override fun visitReturnStatement(o: TolkReturnStatement) {
                        hasReturn = true
                        stopWalking()
                    }
                })
                if (hasReturn) {
                    return false
                }
            }
            modificationTracker.incModificationCount()
        }
        return shouldInc
    }

    override fun toString(): String = "TolkFunction:$name"

    companion object {
        val LOG = logger<TolkFunctionMixin>()
    }
}

private val FUNCTION_TYPE = Key.create<CachedValue<TolkTyFunction>>("tolk.function.function_type")
private val RETURN_TYPE_KEY = Key.create<CachedValue<TolkTy>>("tolk.function.return_type")
private val RECEIVER_TYPE_KEY = Key.create<CachedValue<TolkTy>>("tolk.function.receiver_type")

private fun TolkFunction.resolveReturnType(): TolkTy {
    val returnTypePsi = returnType
    if (returnTypePsi != null) {
        return if (returnTypePsi.selfKeyword != null) {
            receiverTy
        } else {
            val typeExExpression = returnTypePsi.typeExpression
            if (typeExExpression != null) {
                typeExExpression.type ?: TolkTy.Unknown
            } else {
                TolkTy.Unknown
            }
        }
    }

    val statements = SmartList<TolkReturnStatement>()
    val visitor = object : TolkRecursiveElementWalkingVisitor() {
        override fun visitExpressionStatement(o: TolkExpressionStatement) {
        }

        override fun visitReturnStatement(o: TolkReturnStatement) {
            statements.add(o)
        }
    }
    functionBody?.blockStatement?.accept(visitor)

    if (statements.isEmpty() || statements.all { it.expression == null }) {
        return TolkTy.Void
    }

    val inference = try {
        inference
    } catch (e: CyclicReferenceException) {
        null
    } ?: return TolkTy.Unknown
    val result = if (inference.returnStatements.isNotEmpty()) {
        inference.returnStatements.asSequence().map {
            it.expression?.type
        }.filterNotNull().fold<TolkTy, TolkTy?>(null) { a, b ->
            a?.join(b) ?: b
        } ?: TolkTy.Void
    } else {
        TolkTy.Void
    }
    return result
}

val TolkFunction.declaredType: TolkTyFunction get() = (this as TolkFunctionMixin).type

val TolkFunction.isMutable: Boolean
    get() = greenStub?.isMutable ?: (node.findChildByType(TolkElementTypes.TILDE) != null)

val TolkFunction.getKeyword get() = node.findChildByType(TolkElementTypes.GET_KEYWORD)

val TolkFunction.isGetMethod: Boolean
    get() = greenStub?.isGetMethod
        ?: (getKeyword != null || annotations.hasAnnotation("method_id"))

val TolkFunction.hasDeprecatedAnnotation: Boolean
    get() = greenStub?.isDeprecated ?: annotations.hasDeprecatedAnnotation()

val TolkFunction.isEntryPoint: Boolean
    get() {
        return when (name ?: return false) {
            "main",
            "onInternalMessage",
            "onExternalMessage",
            "onRunTickTock",
            "onSplitPrepare",
            "onBouncedMessage",
            "onSplitInstall" -> true

            else -> false
        }
    }

val TolkFunction.hasAsm: Boolean
    get() = greenStub?.hasAsm ?: (functionBody?.asmDefinition != null)

val TolkFunction.isBuiltin: Boolean
    get() = greenStub?.isBuiltin ?: (functionBody?.builtinKeyword != null)

val TolkFunction.hasSelf: Boolean
    get() = greenStub?.hasSelf ?: (parameterList?.selfParameter != null)

val TolkFunction.hasReceiver: Boolean
    get() = greenStub?.hasReceiver ?: (functionReceiver != null)

val TolkFunction.isMethod: Boolean
    get() = hasSelf && hasReceiver

val TolkFunction.isStatic: Boolean
    get() = !hasSelf && hasReceiver

val TolkFunction.returnTy get() = (this as TolkFunctionMixin).returnTy

val TolkFunction.receiverTy get() = (this as TolkFunctionMixin).receiverTy
