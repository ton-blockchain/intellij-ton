package org.ton.intellij.tolk.psi.impl

import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.jetbrains.kotlin.idea.completion.handlers.indexOfSkippingSpace
import org.ton.intellij.tolk.TolkIcons
import org.ton.intellij.tolk.ide.completion.TolkCompletionContributor
import org.ton.intellij.tolk.psi.TolkElementTypes
import org.ton.intellij.tolk.psi.TolkFunction
import org.ton.intellij.tolk.psi.TolkParameter
import org.ton.intellij.tolk.psi.TolkTypeParameter
import org.ton.intellij.tolk.stub.TolkFunctionStub
import org.ton.intellij.tolk.type.*
import org.ton.intellij.util.greenStub
import javax.swing.Icon

abstract class TolkFunctionMixin : TolkNamedElementImpl<TolkFunctionStub>, TolkFunction {
    constructor(node: ASTNode) : super(node)

    constructor(stub: TolkFunctionStub, stubType: IStubElementType<*, *>) : super(stub, stubType)

    override fun getIcon(flags: Int): Icon? {
        return TolkIcons.FUNCTION
    }

    override fun toString(): String = "TolkFunction($containingFile - $name)"

    override fun getBaseIcon(): Icon? = TolkIcons.FUNCTION

    val parameters: List<TolkParameter>
        get() = parameterList?.parameterList ?: emptyList()

    override val type: TolkFunctionType?
        get() = CachedValuesManager.getCachedValue(this) {
            val parameters = parameters.map { parameter ->
                parameter.typeExpression?.type ?: return@getCachedValue null.also {
//                    println("[${containingFile.name}] Failed to get type for $parameter - `${parameter.text}`")
                }
            }
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
        }?.also {
//            println("${name} returnType: $it ||| ${buildString { it.printDisplayName(this) }}")
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

val TolkFunction.isGeneric: Boolean
    get() = greenStub?.isGeneric ?: typeParameterList?.typeParameterList?.isNotEmpty() ?: false

val TolkFunction.hasSelf: Boolean
    get() = greenStub?.hasSelf ?: (parameterList?.parameterList?.firstOrNull()?.let {
        it.name == "self" && it.typeExpression == null
    } ?: false)

val TolkFunction.parameters: List<TolkParameter>
    get() = (this as? TolkFunctionMixin)?.parameters ?: (parameterList?.parameterList ?: emptyList())

fun TolkFunction.toLookupElement(): LookupElement {
    return PrioritizedLookupElement.withPriority(
        LookupElementBuilder.createWithIcon(this)
            // Добавляем тип возвращаемого значения
            .withTypeText((this.type as? TolkFunctionType)?.returnType?.let {
                buildString { it.printDisplayName(this) }
            } ?: "_")
            // Добавляем типовые параметры, если есть
            .let { builder ->
                typeParameterList?.let { list ->
                    builder.appendTailText(
                        list.typeParameterList.joinToString(
                            prefix = "<",
                            postfix = ">"
                        ) { it.name.toString() },
                        true
                    )
                } ?: builder
            }
            // Добавляем параметры функции
            .let { builder ->
                builder.appendTailText(
                    parameterList?.parameterList?.joinToString(
                        prefix = "(",
                        postfix = ")"
                    ) {
                        buildString {
                            append(it.name)
                            append(": ")
                            it.typeExpression?.type?.printDisplayName(this) ?: append("_")
                        }
                    } ?: "()",
                    true
                )
            }
            // Добавляем обработчик вставки
            .withInsertHandler { context, item ->
                val offset = context.editor.caretModel.offset
                val chars = context.document.charsSequence

                val hasOpenBracket = chars.indexOfSkippingSpace('(', offset) != null

                if (!hasOpenBracket) {
                    val offset = if (this.parameters.isEmpty()) 2 else 1
                    context.document.insertString(context.editor.caretModel.offset, "()")
                    context.editor.caretModel.moveToOffset(context.editor.caretModel.offset + offset)
                }

                context.commitDocument()
            },
        TolkCompletionContributor.FUNCTION_PRIORITY
    )
}

fun TolkFunction.resolveGenerics(
    callableType: TolkFunctionType,
    typeArguments: List<TolkType>? = null
): TolkFunctionType {
    val thisType = type as? TolkFunctionType ?: TolkFunctionType(TolkType.Unknown, TolkType.Unknown)
    val mapping = HashMap<TolkTypeParameter, TolkType>()
    typeParameterList?.typeParameterList?.forEachIndexed { index, typeParameter ->
        val typeArgument = typeArguments?.getOrNull(index)
        if (typeArgument != null) {
            mapping[typeParameter] = typeArgument
        }
    }
//    val arguments = callableType.parameters
//    val parameters = thisType.parameters
//    arguments.forEachIndexed {  index, argumentType ->
//        val parameter = parameters.getOrNull(index)
//        if (parameter is TolkType.ParameterType) {
//            mapping[parameter.psiElement] = argumentType
//        }
//    }

    fun resolve(paramType: TolkType, argType: TolkType) {
        when {
            paramType is TolkFunctionType && argType is TolkFunctionType -> {
                resolve(paramType.inputType, argType.inputType)
                resolve(paramType.returnType, argType.returnType)
            }

            paramType is TolkTensorType && argType is TolkTensorType -> {
                paramType.elements.zip(argType.elements).forEach { (a, b) -> resolve(a, b) }
            }

            paramType is TolkTypedTupleType && argType is TolkTypedTupleType -> {
                paramType.elements.zip(argType.elements).forEach { (a, b) -> resolve(a, b) }
            }

            paramType is TolkUnionType && argType is TolkUnionType -> {
                paramType.variants.zip(argType.variants).forEach { (a, b) ->
                    resolve(a, b)
                }
            }

            paramType is TolkType.ParameterType -> {
                if (!mapping.containsKey(paramType.psiElement)) {
                    mapping[paramType.psiElement] = argType
                }
            }
        }
    }

    resolve(thisType, callableType)
    return thisType.substitute(mapping)
}
