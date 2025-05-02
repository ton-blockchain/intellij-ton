package org.ton.intellij.tolk.psi.impl

import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.ton.intellij.tolk.TolkIcons
import org.ton.intellij.tolk.ide.completion.TolkCompletionContributor
import org.ton.intellij.tolk.presentation.TolkPsiRenderer
import org.ton.intellij.tolk.presentation.renderParameterList
import org.ton.intellij.tolk.presentation.renderTypeExpression
import org.ton.intellij.tolk.psi.TolkElement
import org.ton.intellij.tolk.psi.TolkElementTypes
import org.ton.intellij.tolk.psi.TolkFunction
import org.ton.intellij.tolk.psi.TolkParameter
import org.ton.intellij.tolk.stub.TolkFunctionStub
import org.ton.intellij.tolk.type.*
import org.ton.intellij.util.greenStub
import javax.swing.Icon

abstract class TolkFunctionMixin : TolkNamedElementImpl<TolkFunctionStub>, TolkFunction {
    constructor(node: ASTNode) : super(node)

    constructor(stub: TolkFunctionStub, stubType: IStubElementType<*, *>) : super(stub, stubType)

    override fun getIcon(flags: Int): Icon? {
        if (hasSelf) {
            return TolkIcons.METHOD
        }
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
            val returnType = actualReturnType ?: return@getCachedValue null
            val type = TolkFunctionType(TolkType.tensor(parameters), returnType)

            CachedValueProvider.Result.create(type, this)
        }

    private val actualReturnType: TolkType?
        get() = CachedValuesManager.getCachedValue(this) {
            CachedValueProvider.Result.create(resolveReturnType(), this)
        }

    private fun resolveReturnType(): TolkType? {
        val returnTypePsi = returnType
        if (returnTypePsi != null) {
            return if (returnTypePsi.selfKeyword != null) {
                functionReceiver?.typeExpression?.type
            } else {
                returnTypePsi.typeExpression?.type
            }
        }
        val inference = try {
            inference
        } catch (_: CyclicReferenceException) { null } ?: return null
        val result = if (inference.returnStatements.isNotEmpty()) {
            inference.returnStatements.asSequence().map {
                it.expression?.type
            }.filterNotNull().fold(null) { a, b -> a?.join(b) ?: b }
        } else if (inference.unreachable == TolkUnreachableKind.ThrowStatement) {
            TolkType.Never
        } else {
            TolkType.Unit
        }
        return result
    }
}

val TolkFunction.isMutable: Boolean
    get() = greenStub?.isMutable ?: (node.findChildByType(TolkElementTypes.TILDE) != null)

val TolkFunction.isDeprecated: Boolean
    get() = greenStub?.isDeprecated ?: annotationList.any { it.identifier?.textMatches("deprecated") == true }

val TolkFunction.getKeyword get() = node.findChildByType(TolkElementTypes.GET_KEYWORD)

val TolkFunction.isGetMethod: Boolean
    get() = greenStub?.isGetMethod
        ?: (getKeyword != null || annotationList.any { it.identifier?.textMatches("method_id") == true })

val TolkFunction.hasAsm: Boolean
    get() = greenStub?.hasAsm ?: (functionBody?.asmDefinition != null)

val TolkFunction.isBuiltin: Boolean
    get() = greenStub?.isBuiltin ?: (functionBody?.builtinKeyword != null)

val TolkFunction.isGeneric: Boolean
    get() = greenStub?.isGeneric ?: typeParameterList?.typeParameterList?.isNotEmpty() ?: false

val TolkFunction.hasSelf: Boolean
    get() = greenStub?.hasSelf ?: (parameterList?.selfParameter != null)

val TolkFunction.parameters: List<TolkParameter>
    get() = (this as? TolkFunctionMixin)?.parameters ?: (parameterList?.parameterList ?: emptyList())

fun TolkFunction.toLookupElement(): LookupElement {
    return PrioritizedLookupElement.withPriority(
        LookupElementBuilder.createWithIcon(this)
            .withTypeText((this.type as? TolkFunctionType)?.returnType?.let {
                buildString { it.renderAppendable(this) }
            } ?: "_")
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
            .withTailText(getTailText())
            .appendTailText(getExtraTailText(), true)
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

private fun TolkFunction.getTailText(): String {
    val parameterList = parameterList ?: return "()"
    return TolkPsiRenderer().renderParameterList(parameterList)
}

private fun TolkFunction.getExtraTailText(): String {
    val receiver = functionReceiver?.typeExpression ?: return ""
    return " of ${TolkPsiRenderer().renderTypeExpression(receiver)}"
}

fun TolkFunction.resolveGenerics(
    callableType: TolkFunctionType,
    typeArguments: List<TolkType>? = null
): TolkFunctionType {
    val thisType = type as? TolkFunctionType ?: TolkFunctionType(TolkType.Unknown, TolkType.Unknown)
    val mapping = HashMap<TolkElement, TolkType>()
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

            paramType is TolkType.GenericType -> {
                if (!mapping.containsKey(paramType.psiElement)) {
                    mapping[paramType.psiElement] = argType
                }
            }
        }
    }

    resolve(thisType, callableType)
    return thisType.substitute(mapping)
}

private fun CharSequence.indexOfSkippingSpace(c: Char, startIndex: Int): Int? {
    for (i in startIndex until this.length) {
        val currentChar = this[i]
        if (c == currentChar) return i
        if (currentChar != ' ' && currentChar != '\t') return null
    }

    return null
}
