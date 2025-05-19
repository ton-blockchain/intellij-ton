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
import org.ton.intellij.tolk.perf
import org.ton.intellij.tolk.presentation.TolkPsiRenderer
import org.ton.intellij.tolk.presentation.renderParameterList
import org.ton.intellij.tolk.presentation.renderTypeExpression
import org.ton.intellij.tolk.psi.TolkElementTypes
import org.ton.intellij.tolk.psi.TolkFile
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

    override val type: TolkFunctionTy
        get() = CachedValuesManager.getCachedValue(this) {
            val parameters = parameters.map { parameter ->
                parameter.typeExpression.type ?: TolkTy.Unknown
            }
            val parameterType = TolkTy.tensor(parameters)
            val returnType = resolveReturnType() ?: TolkTy.Unknown
            val type = TolkFunctionTy(parameterType, returnType)

            CachedValueProvider.Result.create(type, this)
        }

    private fun resolveReturnType(): TolkTy? {
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
        } catch (e: CyclicReferenceException) {
            null
        } ?: return null
        val result = if (inference.returnStatements.isNotEmpty()) {
            inference.returnStatements.asSequence().map {
                it.expression?.type
            }.filterNotNull().fold(null) { a, b -> a?.join(b) ?: b }
        } else if (inference.unreachable == TolkUnreachableKind.ThrowStatement) {
            TolkTy.Never
        } else {
            TolkTy.Unit
        }
        return result
    }
}

val TolkFunction.declaredType: TolkFunctionTy get() = (this as TolkFunctionMixin).type

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
    val typeText = perf("type text") {
//        val type = perf("get function type") {
////            (returnType?.typeExpression?.type ?: (this.type as? TolkFunctionTy)?.returnType)
//            returnType?.typeExpression?.type // triggers inference for all project, causes lags
//        }
//        perf("render function type") {
//            type?.render()
//        } ?: "<unknown>"
        returnType?.typeExpression?.text // triggers inference for all project, causes lags
    }
    return PrioritizedLookupElement.withPriority(
        LookupElementBuilder.createWithIcon(this)
            .withTypeText(typeText)
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
                    context.commitDocument()
                }

                val insertFile = context.file as? TolkFile ?: return@withInsertHandler
                val includeCandidateFile = this.originalElement.containingFile as? TolkFile ?: return@withInsertHandler
                insertFile.import(includeCandidateFile)
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

private fun CharSequence.indexOfSkippingSpace(c: Char, startIndex: Int): Int? {
    for (i in startIndex until this.length) {
        val currentChar = this[i]
        if (c == currentChar) return i
        if (currentChar != ' ' && currentChar != '\t') return null
    }

    return null
}
