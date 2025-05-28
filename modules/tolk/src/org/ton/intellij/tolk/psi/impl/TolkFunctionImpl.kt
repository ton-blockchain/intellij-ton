package org.ton.intellij.tolk.psi.impl

import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.Key
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.util.*
import org.ton.intellij.tolk.TolkIcons
import org.ton.intellij.tolk.ide.completion.TolkCompletionContributor
import org.ton.intellij.tolk.perf
import org.ton.intellij.tolk.presentation.TolkPsiRenderer
import org.ton.intellij.tolk.presentation.renderParameterList
import org.ton.intellij.tolk.presentation.renderTypeExpression
import org.ton.intellij.tolk.psi.TolkAnnotation
import org.ton.intellij.tolk.psi.TolkElementTypes
import org.ton.intellij.tolk.psi.TolkFile
import org.ton.intellij.tolk.psi.TolkFunction
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

    override fun getBaseIcon(): Icon? = TolkIcons.FUNCTION

    override val type: TolkFunctionTy
        get() = CachedValuesManager.getCachedValue(this) {
            val returnTy = this@TolkFunctionMixin.returnTy
            val parameterList = parameterList ?: return@getCachedValue CachedValueProvider.Result.create(
                TolkFunctionTy(
                    TolkTy.Unit,
                    returnTy
                ), this
            )
            val selfParameter = parameterList.selfParameter
            val parameters = parameterList.parameterList
            val tensor: ArrayList<TolkTy>
            if (selfParameter != null) {
                tensor = ArrayList(parameters.size + 1)
                tensor.add(selfParameter.type ?: TolkTy.Unknown)
            } else {
                tensor = ArrayList(parameters.size)
            }
            parameters.forEach {
                val type = it.typeExpression.type ?: TolkTy.Unknown
                tensor.add(type)
            }

            val parameterTy = TolkTy.tensor(tensor)
            val type = TolkFunctionTy(parameterTy, returnTy)

            CachedValueProvider.Result.create(type, this)
        }

    val returnTy: TolkTy
        get() = CachedValuesManager.getManager(project).getParameterizedCachedValue(
            this, RETURN_TYPE_KEY, RETURN_TYPE_PROVIDER, false, this
        )

    val receiverTy: TolkTy?
        get() = CachedValuesManager.getCachedValue(this) {
            val receiverType = functionReceiver?.typeExpression?.type ?: TolkTy.Unknown
            CachedValueProvider.Result.create(receiverType, this)
        }
}

private val RETURN_TYPE_KEY = Key.create<ParameterizedCachedValue<TolkTy, TolkFunction>>("tolk.function.return_type")
private val RETURN_TYPE_PROVIDER = object : ParameterizedCachedValueProvider<TolkTy, TolkFunction> {
    override fun compute(param: TolkFunction): CachedValueProvider.Result<TolkTy> {
        return CachedValueProvider.Result.create(param.resolveReturnType(), param)
    }

    private fun TolkFunction.resolveReturnType(): TolkTy {
        val returnTypePsi = returnType
        if (returnTypePsi != null) {
            return if (returnTypePsi.selfKeyword != null) {
                functionReceiver?.typeExpression?.type ?: TolkTy.Unknown
            } else {
                returnTypePsi.typeExpression?.type ?: TolkTy.Unknown
            }
        }
        val inference = try {
            inference
        } catch (e: CyclicReferenceException) {
            null
        } ?: return TolkTy.Unknown
        val result = if (inference.unreachable == TolkUnreachableKind.ThrowStatement) {
            TolkTy.Never
        } else if (inference.returnStatements.isNotEmpty()) {
            inference.returnStatements.asSequence().map {
                it.expression?.type
            }.filterNotNull().fold<TolkTy, TolkTy?>(null) { a, b ->
                a?.join(b) ?: b
            } ?: TolkTy.Unit
        } else {
            TolkTy.Unit
        }
        return result
    }
}

val TolkFunction.declaredType: TolkFunctionTy get() = (this as TolkFunctionMixin).type

val TolkFunction.isMutable: Boolean
    get() = greenStub?.isMutable ?: (node.findChildByType(TolkElementTypes.TILDE) != null)

val TolkFunction.annotationList: List<TolkAnnotation>
    get() = PsiTreeUtil.getChildrenOfTypeAsList(this, TolkAnnotation::class.java)

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

val TolkFunction.hasSelf: Boolean
    get() = greenStub?.hasSelf ?: (parameterList?.selfParameter != null)

fun TolkFunction.toLookupElement(): LookupElement {
    val typeText = perf("type text") {
        returnType?.typeExpression?.type?.render()
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
                    val offset = if (parameterList?.parameterList.isNullOrEmpty()) 2 else 1
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
