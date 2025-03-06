package org.ton.intellij.tolk.type

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findPsiFile
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.util.parentOfType
import com.intellij.util.containers.OrderedSet
import org.ton.intellij.tolk.diagnostics.TolkDiagnostic
import org.ton.intellij.tolk.psi.*
import org.ton.intellij.tolk.psi.impl.TolkIncludeDefinitionMixin
import org.ton.intellij.tolk.psi.impl.resolveFile
import org.ton.intellij.util.recursionGuard
import org.ton.intellij.util.tokenSetOf
import java.math.BigInteger
import java.util.*

val TolkElement.inference: TolkInferenceResult?
    get() {
        val context: TolkInferenceContextOwner? =
            this as? TolkInferenceContextOwner ?: context?.parentOfType<TolkInferenceContextOwner>()
        return if (context != null) {
            inferTypesIn(context)
        } else {
            null
        }
    }

fun inferTypesIn(element: TolkInferenceContextOwner): TolkInferenceResult {
    val ctx = TolkInferenceContext(element.project, element)
    return recursionGuard(element, memoize = false) { ctx.infer(element) }
        ?: throw CyclicReferenceException(element)
}

class CyclicReferenceException(val element: TolkInferenceContextOwner) :
    IllegalStateException("Can't do inference on cyclic inference context owner: $element")

interface TolkInferenceData {
    val returnStatements: List<TolkReturnStatement>

    fun getResolvedRefs(element: TolkReferenceExpression): OrderedSet<PsiElementResolveResult>

    fun getType(element: TolkTypedElement?): TolkType?
}

private val EMPTY_RESOLVED_SET = OrderedSet<PsiElementResolveResult>()

data class TolkInferenceResult(
    private val resolvedRefs: Map<TolkReferenceExpression, OrderedSet<PsiElementResolveResult>>,
    private val expressionTypes: Map<TolkTypedElement, TolkType>,
    private val varTypes: Map<TolkVarDefinition, TolkType>,
    override val returnStatements: List<TolkReturnStatement>,
    val unreachable: TolkUnreachableKind? = null
) : TolkInferenceData {
    val timestamp = System.nanoTime()

    override fun getResolvedRefs(element: TolkReferenceExpression): OrderedSet<PsiElementResolveResult> {
        return resolvedRefs[element] ?: EMPTY_RESOLVED_SET
    }

    override fun getType(element: TolkTypedElement?): TolkType? {
        if (element is TolkVarDefinition) {
            return varTypes[element]
        }
        return expressionTypes[element ?: return null]
    }

    companion object
}

class TolkInferenceContext(
    val project: Project,
    val owner: TolkInferenceContextOwner
) : TolkInferenceData {
    private val resolvedRefs = HashMap<TolkReferenceExpression, OrderedSet<PsiElementResolveResult>>()
    private val diagnostics = LinkedList<TolkDiagnostic>()
    private val varTypes = HashMap<TolkVarDefinition, TolkType>()
    internal val expressionTypes = HashMap<TolkTypedElement, TolkType>()
    override val returnStatements = LinkedList<TolkReturnStatement>()
    var declaredReturnType: TolkType? = null

    override fun getResolvedRefs(element: TolkReferenceExpression): OrderedSet<PsiElementResolveResult> {
        return resolvedRefs[element] ?: EMPTY_RESOLVED_SET
    }

    fun setResolvedRefs(element: TolkReferenceExpression, refs: Collection<PsiElementResolveResult>) {
        resolvedRefs[element] = OrderedSet(refs)
    }

    override fun getType(element: TolkTypedElement?): TolkType? {
        if (element is TolkVarDefinition) {
            return varTypes[element]
        }
        return expressionTypes[element]
    }

    fun <T : TolkType> setType(element: TolkVarDefinition, type: T?): T? {
        if (type != null) {
            varTypes[element] = type
        }
        return type
    }

    fun <T : TolkType> setType(element: TolkExpression, type: T?): T? {
        if (type != null) {
            expressionTypes[element] = type
        }
        return type
    }


    fun addDiagnostic(diagnostic: TolkDiagnostic) {
        if (diagnostic.element.containingFile.isPhysical) {
            diagnostics.add(diagnostic)
        }
    }


    fun infer(element: TolkInferenceContextOwner): TolkInferenceResult {
        var unreachable: TolkUnreachableKind? = null
        when (element) {
            is TolkFunction -> {
                val walker = TolkInferenceWalker(this)
                val tolkFile = element.containingFile as? TolkFile
                var flow = TolkFlowContext()

                tolkFile?.let {
                    val commonStdlib =
                        TolkIncludeDefinitionMixin.resolveTolkImport(element.project, tolkFile, "@stdlib/common")
                    if (commonStdlib != null) {
                        val tolkCommonStdlib = commonStdlib.findPsiFile(element.project) as? TolkFile
                        if (tolkCommonStdlib != null && tolkFile != tolkCommonStdlib) {
                            flow = walker.inferFile(tolkCommonStdlib, flow, false)
                        }
                    }
                    flow = walker.inferFile(tolkFile, flow)
                }
                flow = walker.inferFunction(element, flow)
                flow.types.forEach { (typedElement, type) ->
                    when (typedElement) {
                        is TolkExpression -> {
                            expressionTypes[typedElement] = type
                        }

                        is TolkVarDefinition -> {
                            varTypes[typedElement] = type
                        }
                    }
                }
                unreachable = flow.unreachable
            }
        }

        return TolkInferenceResult(resolvedRefs, expressionTypes, varTypes, returnStatements, unreachable)
    }
}

class TolkInferenceWalker(
    val ctx: TolkInferenceContext,
//    val parent: TolkInferenceWalker? = null,
//    val throwableElements: MutableList<TolkThrowStatement> = LinkedList(),
) {
    private val importFiles = LinkedHashSet<VirtualFile>()

    fun inferFile(element: TolkFile, flow: TolkFlowContext, useIncludes: Boolean = true): TolkFlowContext {
        val project = element.project
        var nextFlow = flow
        element.functions.forEach { function ->
            nextFlow.globalSymbols[function.name?.removeSurrounding("`") ?: return@forEach] = function
        }
        element.globalVars.forEach { globalVar ->
            nextFlow.globalSymbols[globalVar.name?.removeSurrounding("`") ?: return@forEach] = globalVar
        }
        element.constVars.forEach { constVar ->
            nextFlow.globalSymbols[constVar.name?.removeSurrounding("`") ?: return@forEach] = constVar
        }
        importFiles.add(element.virtualFile)
        if (useIncludes) {
            element.includeDefinitions.forEach {
                val resolvedFile = it.resolveFile(project)
                if (resolvedFile != null) {
                    val resolvedTolkFile = resolvedFile.findPsiFile(element.project) as? TolkFile
                    if (resolvedTolkFile != null) {
                        nextFlow = inferFile(resolvedTolkFile, nextFlow, false)
                    }
                }
            }
        }
        return nextFlow
    }

    fun inferFunction(element: TolkFunction, flow: TolkFlowContext): TolkFlowContext {
        var nextFlow = flow
        ctx.declaredReturnType = element.typeExpression?.type
        element.typeParameterList?.typeParameterList?.forEach { typeParameter ->
            nextFlow.setType(typeParameter, typeParameter.type ?: TolkType.Unknown)
        }
        element.parameterList?.parameterList?.forEach { functionParameter ->
            nextFlow.setType(functionParameter, functionParameter.type ?: TolkType.Unknown)
        }
        element.functionBody?.blockStatement?.let {
            nextFlow = processBlockStatement(it, nextFlow)
        }
        return nextFlow
    }

    private fun inferStatement(element: TolkStatement, flow: TolkFlowContext): TolkFlowContext {
        return when (element) {
            is TolkBlockStatement -> processBlockStatement(element, flow)
            is TolkReturnStatement -> processReturnStatement(element, flow)
            is TolkIfStatement -> processIfStatement(element, flow)
            is TolkRepeatStatement -> processRepeatStatement(element, flow)
            is TolkWhileStatement -> processWhileStatement(element, flow)
            is TolkDoStatement -> processDoStatement(element, flow)
            is TolkThrowStatement -> processThrowStatement(element, flow)
            is TolkAssertStatement -> processAssertStatement(element, flow)
            is TolkTryStatement -> processTryStatement(element, flow)
            is TolkExpressionStatement -> processExpressionStatement(element, flow)
            is TolkVarStatement -> processVarStatement(element, flow)
            else -> flow
        }
    }

    private fun processBlockStatement(element: TolkBlockStatement, flow: TolkFlowContext): TolkFlowContext {
        var nextFlow = flow
        for (statement in element.statementList) {
            nextFlow = inferStatement(statement, nextFlow)
        }
        return nextFlow
    }

    private fun processReturnStatement(element: TolkReturnStatement, flow: TolkFlowContext): TolkFlowContext {
        var nextFlow = flow
        val expression = element.expression
        if (expression != null) {
            nextFlow = inferExpression(expression, flow, false, ctx.declaredReturnType).outFlow
        }
        nextFlow.unreachable = TolkUnreachableKind.ReturnStatement
        ctx.returnStatements.add(element)
        return nextFlow
    }

    private fun processIfStatement(element: TolkIfStatement, flow: TolkFlowContext): TolkFlowContext {
        val condition = element.condition ?: return flow
        val afterCondition = inferExpression(condition, flow, true)
        val trueFlow = element.blockStatement?.let {
            processBlockStatement(it, afterCondition.trueFlow)
        }
        val falseFlow = element.elseBranch?.statement?.let {
            inferStatement(it, afterCondition.falseFlow)
        }

        if (trueFlow == null && falseFlow == null) {
            return afterCondition.outFlow
        }

        if (trueFlow != null && trueFlow.unreachable != null) {
            return falseFlow?.merge(trueFlow) ?: afterCondition.falseFlow.merge(trueFlow)
        }

        return (trueFlow ?: afterCondition.trueFlow).join(falseFlow ?: afterCondition.falseFlow)

//        val nextFlow = if (trueFlow.unreachable != null) {
//            falseFlow.merge(trueFlow)
//            falseFlow
//        } else if (falseFlow.unreachable != null) {
//            trueFlow.merge(falseFlow)
//            trueFlow
//        } else {
//            trueFlow.join(falseFlow)
//        }

//        return nextFlow
    }

    private fun processRepeatStatement(element: TolkRepeatStatement, flow: TolkFlowContext): TolkFlowContext {
        val afterCondition = inferExpression(element.expression ?: return flow, flow, false)
        val body = element.blockStatement ?: return afterCondition.outFlow
        return processBlockStatement(body, afterCondition.outFlow)
    }

    private fun processWhileStatement(element: TolkWhileStatement, flow: TolkFlowContext): TolkFlowContext {
        val condition = element.condition ?: return flow
        val loopEntryFacts = TolkFlowContext(flow)
        val afterCond = inferExpression(condition, loopEntryFacts, true)
        val body = element.blockStatement ?: return afterCond.outFlow
        val bodyOut = processBlockStatement(body, afterCond.trueFlow)

        val nextFlow = loopEntryFacts.join(bodyOut)
        val afterCond2 = inferExpression(condition, nextFlow, true)
        val bodyOut2 = processBlockStatement(body, afterCond2.trueFlow)

        return afterCond2.falseFlow.join(bodyOut2)
    }

    private fun processDoStatement(element: TolkDoStatement, flow: TolkFlowContext): TolkFlowContext {
        val body = element.blockStatement ?: return flow
        val loopEntryFacts = TolkFlowContext(flow)
        var nextFlow = processBlockStatement(body, flow)
        val condition = element.expression ?: return nextFlow
        val afterCond = inferExpression(condition, loopEntryFacts, true)

        nextFlow = loopEntryFacts.join(afterCond.trueFlow)
        nextFlow = processBlockStatement(body, nextFlow)
        val afterCond2 = inferExpression(condition, nextFlow, true)

        return afterCond2.falseFlow
    }

    private fun processThrowStatement(element: TolkThrowStatement, flow: TolkFlowContext): TolkFlowContext {
        val condition = element.expressionList.firstOrNull()
        var nextFlow = flow
        if (condition != null) {
            nextFlow = inferExpression(condition, flow, true).outFlow
            val throwArg = element.expressionList.getOrNull(1)
            if (throwArg != null) {
                nextFlow = inferExpression(throwArg, nextFlow, false).outFlow
            }
        }
        nextFlow.unreachable = TolkUnreachableKind.ThrowStatement
        return flow
    }

    private fun processAssertStatement(element: TolkAssertStatement, flow: TolkFlowContext): TolkFlowContext {
        val expressions = element.expressionList
        val condition = expressions.firstOrNull()
        val throwCode = expressions.getOrNull(1)
        var nextFlow = flow
        if (condition != null) {
            val afterCond = inferExpression(condition, flow, true)
            if (throwCode != null) {
                inferExpression(throwCode, afterCond.falseFlow, false)
            }
            nextFlow = afterCond.trueFlow
        }
        return nextFlow
    }

    private fun processTryStatement(element: TolkTryStatement, flow: TolkFlowContext): TolkFlowContext {
        val tryBody = element.blockStatement ?: return flow
        val catchFlow = TolkFlowContext(flow)

        val tryEnd = processBlockStatement(tryBody, flow)
        val catch = element.catch ?: return tryEnd
        val catchExpr = catch.catchParameterList
        val catchBody = catch.blockStatement ?: return tryEnd

        catchExpr.getOrNull(0)?.let { catchFlow.setType(it, TolkType.Int) }
        catchExpr.getOrNull(1)?.let { catchFlow.setType(it, TolkType.Unknown) }
        val catchEnd = processBlockStatement(catchBody, catchFlow)

        return tryEnd.join(catchEnd)
    }

    private fun processExpressionStatement(element: TolkExpressionStatement, flow: TolkFlowContext): TolkFlowContext {
        val nextFlow = inferExpression(element.expression, flow, false).outFlow
        return nextFlow
    }

    private fun processVarStatement(element: TolkVarStatement, flow: TolkFlowContext): TolkFlowContext {
        val varDefinition = element.varDefinition ?: return flow

        var nextFlow = flow
        nextFlow = processVarDefinition(varDefinition, nextFlow)
        val varDefinitionType = nextFlow.getType(varDefinition)
        val expression = element.expression ?: return nextFlow
        nextFlow = inferExpression(expression, nextFlow, false, varDefinitionType).outFlow
        processVarDefinitionAfterRight(varDefinition, nextFlow.getType(expression) ?: TolkType.Unknown, nextFlow)

        return nextFlow
    }

    private fun processVarDefinition(element: TolkVarDefinition, flow: TolkFlowContext): TolkFlowContext {
        var nextFlow = flow
        when (element) {
            is TolkVarTensor -> {
                val tensorElements = element.varDefinitionList
                val tensorElementTypes = ArrayList<TolkType>(tensorElements.size)
                for (tensorElement in tensorElements) {
                    nextFlow = processVarDefinition(tensorElement, nextFlow)
                    val tensorElementType = nextFlow.getType(tensorElement) ?: TolkType.Unknown
                    tensorElementTypes.add(tensorElementType)
                }
                val tensorType = TolkType.tensor(tensorElementTypes)
                nextFlow.setType(element, tensorType)
            }

            is TolkVarTuple -> {
                val tupleElements = element.varDefinitionList
                val tupleElementTypes = ArrayList<TolkType>(tupleElements.size)
                for (tupleElement in tupleElements) {
                    nextFlow = processVarDefinition(tupleElement, nextFlow)
                    val tupleElementType = nextFlow.getType(tupleElement) ?: TolkType.Unknown
                    tupleElementTypes.add(tupleElementType)
                }
                val tupleType = TolkType.typedTuple(tupleElementTypes)
                nextFlow.setType(element, tupleType)
            }

            is TolkVarRedef -> {
                nextFlow = inferExpression(element.referenceExpression, nextFlow, false).outFlow
                val type = ctx.getType(element.referenceExpression)
                nextFlow.setType(element, type)
            }

            is TolkVar -> {
                val typeHint = element.typeExpression?.type ?: TolkType.Unknown
                nextFlow.setType(element, typeHint)
            }
        }
        return nextFlow
    }

    private fun processVarDefinitionAfterRight(
        element: TolkVarDefinition,
        rightType: TolkType,
        outFlow: TolkFlowContext
    ) {
        when (element) {
            is TolkVar -> {
                outFlow.setType(element, rightType)
            }

            is TolkVarTensor -> {
                val rightElements = if (rightType is TolkTensorType) rightType.elements else listOf(rightType)
                val leftElements = element.varDefinitionList
                val typesList = ArrayList<TolkType>(rightElements.size)
                leftElements.forEachIndexed { index, expression ->
                    val ithRightType = rightElements.getOrNull(index) ?: TolkType.Unknown
                    processVarDefinitionAfterRight(expression, ithRightType, outFlow)
                    typesList.add(outFlow.getType(expression) ?: TolkType.Unknown)
                }
                val tensorType = TolkType.tensor(typesList)
                outFlow.setType(element, tensorType)
            }

            is TolkVarTuple -> {
                val rightElements = if (rightType is TolkTypedTupleType) rightType.elements else listOf(rightType)
                val leftElements = element.varDefinitionList
                val typesList = ArrayList<TolkType>(rightElements.size)
                leftElements.forEachIndexed { index, expression ->
                    val ithRightType = rightElements.getOrNull(index) ?: TolkType.Unknown
                    processVarDefinitionAfterRight(expression, ithRightType, outFlow)
                    typesList.add(outFlow.getType(expression) ?: TolkType.Unknown)
                }
                val tupleType = TolkType.typedTuple(typesList)
                outFlow.setType(element, tupleType)
            }
        }
    }

    private fun inferExpression(
        element: TolkExpression,
        flow: TolkFlowContext,
        usedAsCondition: Boolean,
        hint: TolkType? = null
    ): TolkExpressionFlowContext {
        return when (element) {
            is TolkLiteralExpression -> inferLiteralExpression(element, flow, usedAsCondition)
            is TolkTernaryExpression -> inferTernaryExpression(element, flow, usedAsCondition, hint)
            is TolkBinExpression -> inferBinExpression(element, flow, usedAsCondition)
            is TolkReferenceExpression -> inferReferenceExpression(element, flow, usedAsCondition)
            is TolkTupleExpression -> inferTupleExpression(element, flow, usedAsCondition, hint)
            is TolkTensorExpression -> inferTensorExpression(element, flow, usedAsCondition, hint)
            is TolkCallExpression -> inferCallExpression(element, flow, usedAsCondition, hint)
            is TolkDotExpression -> inferDotExpression(element, flow, usedAsCondition, hint)
            is TolkParenExpression -> inferParenExpression(element, flow, usedAsCondition, hint)
            is TolkPrefixExpression -> inferPrefixExpression(element, flow, usedAsCondition, hint)
            is TolkNotNullExpression -> inferNotNullExpression(element, flow, usedAsCondition)
            is TolkAsExpression -> inferAsExpression(element, flow, usedAsCondition)
//            else -> error("Can't infer type of ${element.type} ${element::class}")
            else -> TolkExpressionFlowContext(flow, usedAsCondition)
        }
    }

    private fun inferLiteralExpression(
        element: TolkLiteralExpression,
        flow: TolkFlowContext,
        usedAsCondition: Boolean
    ): TolkExpressionFlowContext {
        val type = element.type
        flow.setType(element, element.type)
        val after = TolkExpressionFlowContext(flow, usedAsCondition)
        if (usedAsCondition) {
            val boolValue = when (type) {
                TolkType.TRUE -> true
                TolkType.FALSE -> false
                is TolkConstantIntType -> type.value != BigInteger.ZERO
                else -> return after
            }
            if (boolValue) {
                after.falseFlow.unreachable = TolkUnreachableKind.CantHappen
            } else {
                after.trueFlow.unreachable = TolkUnreachableKind.CantHappen
            }
        }
        return after
    }

    private fun inferTernaryExpression(
        element: TolkTernaryExpression,
        flow: TolkFlowContext,
        usedAsCondition: Boolean,
        hint: TolkType?
    ): TolkExpressionFlowContext {
        val condition = element.condition
        val afterCondition = inferExpression(condition, flow, true)
        val thenBranch = element.thenBranch
        val elseBranch = element.elseBranch
        val afterTrue =
            inferExpression(thenBranch ?: return afterCondition, afterCondition.trueFlow, usedAsCondition, hint)
        val afterFalse = inferExpression(
            elseBranch ?: return afterCondition,
            afterCondition.falseFlow,
            usedAsCondition,
            hint
        )

        val conditionType = afterCondition.outFlow.getType(condition)
        if (conditionType == TolkType.TRUE) {
            afterTrue.outFlow.setType(
                element,
                afterCondition.trueFlow.getType(thenBranch)
            )
            return afterTrue
        }

        if (conditionType == TolkType.FALSE) {
            afterFalse.outFlow.setType(
                element,
                afterCondition.falseFlow.getType(elseBranch)
            )
            return afterFalse
        }

        val thenType = afterCondition.trueFlow.getType(thenBranch) ?: TolkType.Unknown
        val elseType = afterCondition.falseFlow.getType(elseBranch) ?: TolkType.Unknown
        val resultType = thenType.join(elseType)

        val outFlow = afterTrue.outFlow.join(afterFalse.outFlow)
        outFlow.setType(element, resultType)

        return TolkExpressionFlowContext(outFlow, afterTrue.trueFlow, afterFalse.falseFlow)
    }

    private val COMPARSION_OPERATORS = tokenSetOf(
        TolkElementTypes.EQEQ,
        TolkElementTypes.NEQ,
        TolkElementTypes.LT,
        TolkElementTypes.GT,
        TolkElementTypes.LEQ,
        TolkElementTypes.GEQ,
        TolkElementTypes.SPACESHIP
    )
    private val AND_OR_XOR_OPERATORS = tokenSetOf(
        TolkElementTypes.AND,
        TolkElementTypes.OR,
        TolkElementTypes.XOR
    )
    private val ASIGMENT_OPERATORS = tokenSetOf(
        TolkElementTypes.PLUSLET,
        TolkElementTypes.MINUSLET,
        TolkElementTypes.TIMESLET,
        TolkElementTypes.DIVLET,
        TolkElementTypes.DIVCLET,
        TolkElementTypes.DIVRLET,
        TolkElementTypes.MODLET,
        TolkElementTypes.MODRLET,
        TolkElementTypes.MODCLET,
        TolkElementTypes.LSHIFTLET,
        TolkElementTypes.RSHIFTCLET,
        TolkElementTypes.RSHIFTRLET,
        TolkElementTypes.RSHIFTLET,
        TolkElementTypes.ANDLET,
        TolkElementTypes.XORLET,
        TolkElementTypes.ORLET,
    )

    private fun inferBinExpression(
        element: TolkBinExpression,
        flow: TolkFlowContext,
        usedAsCondition: Boolean
    ): TolkExpressionFlowContext {
        val binaryOp = element.binaryOp
        val operatorType = binaryOp.node.firstChildNode.elementType

        fun TolkFlowContext.toResult() = TolkExpressionFlowContext(this, usedAsCondition)

        val left = element.left
        val right = element.right
        var nextFlow = flow
        when (operatorType) {
            in COMPARSION_OPERATORS -> {
                val leftType = left.node.firstChildNode.elementType
                val rightType = right?.node?.firstChildNode?.elementType

                val isInverted = operatorType == TolkElementTypes.NEQ
                if (isInverted || operatorType == TolkElementTypes.EQEQ) {
                    if (leftType == TolkElementTypes.NULL_KEYWORD && right != null) {
                        return inferIsNullCheck(element, right, isInverted, nextFlow, usedAsCondition)
                    } else if (rightType == TolkElementTypes.NULL_KEYWORD) {
                        return inferIsNullCheck(element, left, isInverted, nextFlow, usedAsCondition)
                    }
                }

                nextFlow = inferExpression(left, nextFlow, false).outFlow
                nextFlow = inferExpression(right ?: return nextFlow.toResult(), nextFlow, false).outFlow
                nextFlow.setType(element, TolkType.Bool)
            }

            in AND_OR_XOR_OPERATORS -> {
                nextFlow = inferExpression(left, nextFlow, true).outFlow
                if (right != null) {
                    nextFlow = inferExpression(right, nextFlow, true).outFlow
                }
                val leftType = nextFlow.getType(left) ?: TolkType.Unknown
                val rightType = right?.let { nextFlow.getType(right) } ?: TolkType.Unknown

                val elementType = if (leftType is TolkConstantBoolType && rightType is TolkConstantBoolType) {
                    when (operatorType) {
                        TolkElementTypes.AND -> TolkType.bool(leftType.value.and(rightType.value))
                        TolkElementTypes.OR -> TolkType.bool(leftType.value.or(rightType.value))
                        TolkElementTypes.XOR -> TolkType.bool(leftType.value.xor(rightType.value))
                        else -> leftType.join(rightType)
                    }
                } else {
                    leftType.join(rightType)
                }

                nextFlow.setType(element, elementType)
            }

            TolkElementTypes.ANDAND -> {
                nextFlow.setType(element, TolkType.Bool)
                val afterLeft = inferExpression(left, nextFlow, true)
                if (right == null) {
                    return afterLeft.outFlow.toResult()
                }
                val afterRight = inferExpression(right, afterLeft.trueFlow, true)
                if (!usedAsCondition) {
                    val outFlow = afterLeft.falseFlow.join(afterRight.outFlow)
                    val leftType = outFlow.getType(left) ?: TolkType.Unknown
                    val rightType = outFlow.getType(right) ?: TolkType.Unknown
                    val elementType = leftType.join(rightType)
                    outFlow.setType(element, elementType)
                    return TolkExpressionFlowContext(outFlow, false)
                }
                val outFlow = afterLeft.outFlow.join(afterRight.outFlow)
                val leftType = outFlow.getType(left) ?: TolkType.Unknown
                val rightType = outFlow.getType(right) ?: TolkType.Unknown
                val trueFlow = afterRight.trueFlow
                val falseFlow = afterLeft.falseFlow.join(afterRight.falseFlow)
                val elementType = leftType.join(rightType)
                outFlow.setType(element, elementType)
                return TolkExpressionFlowContext(outFlow, trueFlow, falseFlow)
            }

            TolkElementTypes.OROR -> {
                nextFlow.setType(element, TolkType.Bool)
                val afterLeft = inferExpression(left, nextFlow, true)
                if (right == null) {
                    return afterLeft.outFlow.toResult()
                }
                val afterRight = inferExpression(right, afterLeft.falseFlow, true)
                if (!usedAsCondition) {
                    val outFlow = afterLeft.trueFlow.join(afterRight.outFlow)
                    val leftType = outFlow.getType(left) ?: TolkType.Unknown
                    val rightType = outFlow.getType(right) ?: TolkType.Unknown
                    val elementType = leftType.join(rightType)
                    outFlow.setType(element, elementType)
                    return TolkExpressionFlowContext(outFlow, false)
                }
                val outFlow = afterLeft.outFlow.join(afterRight.outFlow)
                val trueFlow = afterLeft.trueFlow.join(afterRight.trueFlow)
                val falseFlow = afterRight.falseFlow
                val leftType = outFlow.getType(left) ?: TolkType.Unknown
                val rightType = outFlow.getType(right) ?: TolkType.Unknown
                val elementType = leftType.join(rightType)
                outFlow.setType(element, elementType)

                return TolkExpressionFlowContext(outFlow, trueFlow, falseFlow)
            }

            TolkElementTypes.EQ -> {
                nextFlow = inferExpression(left, nextFlow, false).outFlow
                if (right == null) {
                    return nextFlow.toResult()
                }
                nextFlow = inferExpression(right, nextFlow, false).outFlow
                val rightType = nextFlow.getType(right) ?: TolkType.Unknown
                processAssigmentAfterRight(left, rightType, nextFlow)
            }

            in ASIGMENT_OPERATORS -> {
                nextFlow = inferExpression(left, nextFlow, false).outFlow
                if (right == null) {
                    return nextFlow.toResult()
                }
                nextFlow = inferExpression(right, nextFlow, false).outFlow
                val leftType = ctx.getType(left) ?: TolkType.Unknown
                val rightType = ctx.getType(right) ?: TolkType.Unknown
            }

            else -> {
                nextFlow = inferExpression(left, nextFlow, false).outFlow
                if (right == null) {
                    nextFlow.setType(element, TolkType.Int)
                    return nextFlow.toResult()
                }
                nextFlow = inferExpression(right, nextFlow, false).outFlow
                val leftType = nextFlow.getType(left)
                val rightType = nextFlow.getType(right)

                if (leftType != null && rightType != null) {
                    nextFlow.setType(element, leftType.join(rightType))
                } else {
                    nextFlow.setType(element, leftType ?: rightType)
                }
            }
        }

        return nextFlow.toResult()
    }

    private fun inferIsNullCheck(
        element: TolkBinExpression,
        expression: TolkExpression,
        isInverted: Boolean,
        flow: TolkFlowContext,
        usedAsCondition: Boolean
    ): TolkExpressionFlowContext {
        val afterExpr = inferExpression(expression, flow, false)

        val exprType = afterExpr.outFlow.getType(expression) ?: return afterExpr
        val notNullType = exprType.removeNullability()
        val resultType = if (exprType == TolkType.Null) { // `expr == null` is always true
            if (isInverted) TolkType.FALSE else TolkType.TRUE
        } else if (notNullType == TolkType.Never || notNullType == exprType) { // `expr == null` is always false
            if (isInverted) TolkType.TRUE else TolkType.FALSE
        } else {
            TolkType.Bool
        }
        afterExpr.outFlow.setType(element, resultType)

        if (!usedAsCondition) {
            return afterExpr
        }

        val trueFlow = TolkFlowContext(afterExpr.outFlow)
        val falseFlow = TolkFlowContext(afterExpr.outFlow)
        val sExpr = extractSinkExpression(expression) ?: return afterExpr
        if (resultType == TolkType.TRUE) {
            falseFlow.unreachable = TolkUnreachableKind.CantHappen
            falseFlow.setType(sExpr, TolkType.Never)
        } else if (resultType == TolkType.FALSE) {
            trueFlow.unreachable = TolkUnreachableKind.CantHappen
            trueFlow.setType(sExpr, TolkType.Never)
        } else if (!isInverted) {
            trueFlow.setType(sExpr, TolkType.Null)
            falseFlow.setType(sExpr, notNullType)
        } else {
            trueFlow.setType(sExpr, notNullType)
            falseFlow.setType(sExpr, TolkType.Null)
        }

        return TolkExpressionFlowContext(afterExpr.outFlow, trueFlow, falseFlow)
    }


    private fun inferLeftSideAssigment(
        element: TolkExpression,
        flow: TolkFlowContext,
    ): TolkFlowContext {
        if (element !is TolkTensorExpression) return flow
        var nextFlow = flow
        // TODO
        return nextFlow
    }

    private fun processAssigmentAfterRight(
        left: TolkExpression,
        rightType: TolkType,
        flow: TolkFlowContext
    ) {
        val leftType = flow.getType(left)
        val joinType = leftType?.join(rightType) ?: rightType

        when (left) {
            is TolkReferenceExpression -> {
                val resolvedElement = extractSinkExpression(left)
                if (resolvedElement != null) {
                    flow.setType(resolvedElement, rightType)
                }
                flow.setType(left, rightType)
            }

            is TolkTensorExpression -> {
                if (joinType is TolkTensorType) {
                    left.expressionList.zip(joinType.elements).forEach { (left, right) ->
                        processAssigmentAfterRight(left, right, flow)
                    }
                }
                flow.setType(left, joinType)
            }

            is TolkTupleExpression -> {
                if (joinType is TolkTypedTupleType) {
                    left.expressionList.zip(joinType.elements).forEach { (left, right) ->
                        processAssigmentAfterRight(left, right, flow)
                    }
                }
                flow.setType(left, joinType)
            }

            is TolkParenExpression -> {
                processAssigmentAfterRight(left.expression ?: return, joinType, flow)
                flow.setType(left, joinType)
            }
        }
    }

    private fun inferReferenceExpression(
        element: TolkReferenceExpression,
        flow: TolkFlowContext,
        usedAsCondition: Boolean
    ): TolkExpressionFlowContext {
        val name = element.name
        val symbol = flow.getSymbol(
            element.project, name,
        )
        if (symbol != null) {
            val type = when (symbol) {
                is TolkFunction -> {
                    val symbolType = symbol.type ?: TolkFunctionType(TolkType.Unknown, TolkType.Unknown)
                    val substituteMap = HashMap<TolkTypeParameter, TolkType>()
                    val typeArgumentList = element.typeArgumentList?.typeExpressionList ?: emptyList()
                    val typeParameterList = symbol.typeParameterList?.typeParameterList ?: emptyList()
                    typeParameterList.zip(typeArgumentList).forEach { (typeParameter, typeArgument) ->
                        substituteMap[typeParameter] = typeArgument.type ?: TolkType.Unknown
                    }
                    symbolType.substitute(substituteMap)
                }

                is TolkGlobalVar -> symbol.type
                is TolkConstVar -> symbol.type
                is TolkTypedElement ->
                    flow.getType(symbol)

                else -> null
            }
            flow.setType(element, type)
            ctx.setResolvedRefs(element, listOf(PsiElementResolveResult(symbol)))
        }
        return TolkExpressionFlowContext(flow, usedAsCondition)
    }

    private fun inferTupleExpression(
        element: TolkTupleExpression,
        flow: TolkFlowContext,
        usedAsCondition: Boolean,
        hint: TolkType?
    ): TolkExpressionFlowContext {
        val tupleHint = hint as? TolkTypedTupleType
        val typesList = ArrayList<TolkType>(element.expressionList.size)
        var nextFlow = flow
        for (item in element.expressionList) {
            nextFlow = inferExpression(item, nextFlow, false, tupleHint?.elements?.getOrNull(typesList.size)).outFlow
            val type = nextFlow.getType(item) ?: TolkType.Unknown
            typesList.add(type)
        }
        val tupleType = TolkType.typedTuple(typesList)
        nextFlow.setType(element, tupleType)
        return TolkExpressionFlowContext(nextFlow, usedAsCondition)
    }

    private fun inferTensorExpression(
        element: TolkTensorExpression,
        flow: TolkFlowContext,
        usedAsCondition: Boolean,
        hint: TolkType?
    ): TolkExpressionFlowContext {
        val tensorHint = hint as? TolkTensorType
        val typesList = ArrayList<TolkType>(element.expressionList.size)
        var nextFlow = flow
        for (item in element.expressionList) {
            nextFlow = inferExpression(item, nextFlow, false, tensorHint?.elements?.getOrNull(typesList.size)).outFlow
            val type = nextFlow.getType(item) ?: TolkType.Unknown
            typesList.add(type)
        }
        val tensorType = TolkType.tensor(typesList)
        nextFlow.setType(element, tensorType)
        return TolkExpressionFlowContext(nextFlow, usedAsCondition)
    }

    private fun inferCallExpression(
        element: TolkCallExpression,
        flow: TolkFlowContext,
        usedAsCondition: Boolean,
        hint: TolkType?
    ): TolkExpressionFlowContext {
        var nextFlow = flow
        val callee = element.expression
        nextFlow = inferExpression(callee, nextFlow, false).outFlow
        val functionType = nextFlow.getType(callee)

        val argumentTypes = ArrayList<TolkType>(element.argumentList.argumentList.size + 1)

        if (callee is TolkDotExpression) {
            val firstArgType = nextFlow.getType(callee.left) ?: TolkType.Unknown
            argumentTypes.add(firstArgType)
        }

        element.argumentList.argumentList.forEach { argument ->
            nextFlow = inferExpression(argument.expression, nextFlow, false).outFlow
            val type = nextFlow.getType(argument.expression) ?: TolkType.Unknown
            argumentTypes.add(type)
        }
        val callType = TolkFunctionType(TolkType.tensor(argumentTypes), hint ?: TolkType.Unknown)

        if (functionType != null) {
            val resolvedFunctionType = (functionType as? TolkFunctionType)?.resolveGenerics(callType)
            nextFlow.setType(element, resolvedFunctionType?.returnType)
        } else {
            nextFlow.setType(element, callType.returnType)
        }

        return TolkExpressionFlowContext(nextFlow, usedAsCondition)
    }

    private fun inferDotExpression(
        element: TolkDotExpression,
        flow: TolkFlowContext,
        usedAsCondition: Boolean,
        hint: TolkType?
    ): TolkExpressionFlowContext {
        var nextFlow = flow
        val left = element.left
        nextFlow = inferExpression(left, nextFlow, false).outFlow

        val right = element.right
        if (right != null) {
            nextFlow = inferExpression(right, nextFlow, false).outFlow
            nextFlow.setType(element, nextFlow.getType(right))
        }

        return TolkExpressionFlowContext(nextFlow, usedAsCondition)
    }

    private fun inferParenExpression(
        element: TolkParenExpression,
        flow: TolkFlowContext,
        usedAsCondition: Boolean,
        hint: TolkType?
    ): TolkExpressionFlowContext {
        val expression = element.expression ?: return TolkExpressionFlowContext(flow, usedAsCondition)
        val afterExpr = inferExpression(expression, flow, usedAsCondition, hint)
        afterExpr.outFlow.setType(element, afterExpr.outFlow.getType(expression))
        return afterExpr
    }

    private val INT_PREFIX_OPERATORS = tokenSetOf(
        TolkElementTypes.MINUS,
        TolkElementTypes.PLUS,
        TolkElementTypes.TILDE
    )

    private fun inferPrefixExpression(
        element: TolkPrefixExpression,
        flow: TolkFlowContext,
        usedAsCondition: Boolean,
        hint: TolkType?
    ): TolkExpressionFlowContext {
        val expression = element.expression ?: return TolkExpressionFlowContext(flow, usedAsCondition)
        var afterExpr = inferExpression(expression, flow, usedAsCondition, hint)
        val operatorType = element.node.firstChildNode.elementType
        when (operatorType) {
            TolkElementTypes.MINUS -> {
                val expressionType = afterExpr.outFlow.getType(expression) as? TolkIntType ?: TolkType.Int
                val resultType = expressionType.negate()
                afterExpr.outFlow.setType(element, resultType)
            }

            TolkElementTypes.EXCL -> {
                val expressionType = afterExpr.outFlow.getType(expression) as? TolkBoolType ?: TolkType.Bool
                val resultType = expressionType.negate()
                afterExpr.outFlow.setType(element, resultType)
                afterExpr = TolkExpressionFlowContext(afterExpr.outFlow, afterExpr.falseFlow, afterExpr.trueFlow)
            }

            in INT_PREFIX_OPERATORS -> {
                afterExpr.outFlow.setType(element, TolkType.Int)
            }
        }

        return afterExpr
    }

    private fun inferNotNullExpression(
        element: TolkNotNullExpression,
        flow: TolkFlowContext,
        usedAsCondition: Boolean,
    ): TolkExpressionFlowContext {
        val expression = element.expression
        val afterExpr = inferExpression(expression, flow, false)
        val type = afterExpr.outFlow.getType(expression)
        if (type != null && type.isNullable()) {
            afterExpr.outFlow.setType(element, type.removeNullability())
        } else {
            afterExpr.outFlow.setType(element, type)
        }
        if (!usedAsCondition) {
            return afterExpr
        }

        return TolkExpressionFlowContext(afterExpr.outFlow, true)
    }

    private fun inferAsExpression(
        element: TolkAsExpression,
        flow: TolkFlowContext,
        usedAsCondition: Boolean,
    ): TolkExpressionFlowContext {
        val expression = element.expression
        val asType = element.typeExpression?.type
        val afterExpr = inferExpression(expression, flow, false, asType)
        afterExpr.outFlow.setType(element, asType)
        if (!usedAsCondition) {
            return afterExpr
        }

        return TolkExpressionFlowContext(afterExpr.outFlow, true)
    }


    fun TolkType.tensorElements() = (this as? TolkTensorType)?.elements ?: listOf(this)

    private fun extractSinkExpression(expression: TolkExpression): TolkTypedElement? {
        when (expression) {
            is TolkReferenceExpression -> return ctx.getResolvedRefs(expression)
                .firstOrNull()?.element as? TolkTypedElement

            is TolkParenExpression -> return extractSinkExpression(expression.expression ?: return null)
            else -> return null
        }
    }

//
//    fun infer(element: TolkCatch) {
//        val blockWalker = TolkInferenceWalker(ctx, this)
//        element.catchParameterList.forEachIndexed { index, param ->
//            val name = param.name?.removeSurrounding("`") ?: return@forEachIndexed
//            blockWalker.symbolDefinitions[name] = Symbol(param, if (index == 0) TolkType.Int else null)
//        }
//        element.blockStatement?.let { blockStatement ->
//            blockWalker.infer(blockStatement)
//        }
//    }
//
//    fun infer(element: TolkVarDefinition, typeMap: MutableMap<TolkVarDefinition, TolkType>): TolkType? {
//        return when (element) {
//            is TolkVarRedef -> {
//                val type = infer(element.referenceExpression) ?: return null
//                typeMap[element] = type
//                type
//            }
//
//            is TolkVar -> {
//                val type = element.typeExpression?.type ?: TolkType.Unknown
//                typeMap[element] = type
//                type
//            }
//
//            is TolkVarTuple -> {
//                val tuple = element.varDefinitionList.map {
//                    infer(it, typeMap) ?: TolkType.Unknown
//                }
//                TolkType.TypedTuple(tuple)
//            }
//
//            is TolkVarTensor -> {
//                val tensor = element.varDefinitionList.map {
//                    infer(it, typeMap) ?: TolkType.Unknown
//                }
//                TolkType.create(tensor)
//            }
//
//            else -> null
//        }
//    }
//
//    private fun infer(element: TolkBlockStatement) {
//        element.statementList.forEach { statement ->
//            infer(statement)
//        }
//    }
//
//
//    private fun infer(element: TolkReturnStatement) {
//        element.expression?.let { expression ->
//            val function = element.parentOfType<TolkFunction>()
//            val returnType = function?.typeExpression?.type
//            infer(expression, returnType)
//        }
//        ctx.addReturnStatement(element)
//    }
//
//    private fun infer(element: TolkRepeatStatement) {
//        element.expression?.let { expression ->
//            infer(expression)
//        }
//        element.blockStatement?.let { blockStatement ->
//            TolkInferenceWalker(ctx, this, throwableElements).infer(blockStatement)
//        }
//    }
//
//
//    private fun infer(element: TolkDoStatement) {
//        element.blockStatement?.let { blockStatement ->
//            val walker = TolkInferenceWalker(ctx, this, throwableElements)
//            walker.infer(blockStatement)
//            element.expression?.let { expression ->
//                walker.infer(expression)
//            }
//        }
//    }
//
//    private fun infer(element: TolkWhileStatement) {
//        element.condition?.let { condition ->
//            infer(condition)
//        }
//        element.blockStatement?.let { blockStatement ->
//            TolkInferenceWalker(ctx, this, throwableElements).infer(blockStatement)
//        }
//    }
//
//    private fun infer(element: TolkAssertStatement) {
//        element.assertCondition?.expression?.let { expression ->
//            infer(expression)
//        }
//        element.assertExcNo?.expression?.let { excNo ->
//            infer(excNo)
//        }
//        element.throwStatement?.let { expression ->
//            infer(expression)
//        }
//    }
//
//
//    private fun infer(element: TolkThrowStatement) {
//        throwableElements.add(element)
//        element.expressionList.forEachIndexed { index, expression ->
//            infer(expression, if (index == 0) TolkType.Int else null)
//        }
//    }
//
//    private fun infer(element: TolkTryStatement) {
//        val blockWalker = TolkInferenceWalker(ctx, this)
//        element.blockStatement?.let { blockStatement ->
//            blockWalker.infer(blockStatement)
//        }
//        element.catch?.let { catch ->
//            infer(catch)
//        }
//    }
//
//    fun unify(
//        t1: TolkType,
//        t2: TolkType,
//    ): TolkType? {
//        if (t1 == t2) return t1
//        if (t1 is TolkType.Tensor && t2 is TolkType.Tensor) {
//            val (tt1, tt22) = if (t1.elements.size <= t2.elements.size) t1 to t2 else t2 to t1
//            tt1.elements.zip(tt22.elements).forEach { (e1, e2) ->
//                if (unify(e1, e2) == null) return null
//            }
//            return tt1
//        }
//        if (t1 is TolkType.TypedTuple && t2 is TolkType.TypedTuple) {
//            val (tt1, tt22) = if (t1.elements.size <= t2.elements.size) t1 to t2 else t2 to t1
//            tt1.elements.zip(tt22.elements).forEach { (e1, e2) ->
//                if (unify(e1, e2) == null) return null
//            }
//            return tt1
//        }
//        return null
//    }
//
//    private fun infer(element: TolkVarStatement) {
//        val typeMap = HashMap<TolkVarDefinition, TolkType>()
//        val varType = element.varDefinition?.let { definition ->
//            infer(definition, typeMap)
//        }
//        val expressionType = element.expression?.let { expression ->
//            infer(expression, if (varType !is TolkUnknownType) varType else null)
//        }
//
//        fun unify(
//            element: TolkVarDefinition,
//            tolkType: TolkType
//        ) {
//            when (element) {
//                is TolkVar -> {
//                    typeMap[element] = tolkType
//                }
//
//                is TolkVarTuple -> {
//                    if (tolkType is TolkType.TypedTuple) {
//                        element.varDefinitionList.zip(tolkType.elements).forEach { (e1, e2) ->
//                            unify(e1, e2)
//                        }
//                    } else {
//                        // mismatch
//                    }
//                }
//
//                is TolkVarTensor -> {
//                    if (tolkType is TolkType.Tensor) {
//                        element.varDefinitionList.zip(tolkType.elements).forEach { (e1, e2) ->
//                            unify(e1, e2)
//                        }
//                    } else {
//                        // mismatch
//                    }
//                }
//            }
//        }
//        if (expressionType != null) {
//            element.varDefinition?.let { definition ->
//                unify(definition, expressionType)
//            }
//        }
//        typeMap.forEach { (definition, type) ->
//            if (definition is TolkNamedElement) {
//                val name = definition.name?.removeSurrounding("`") ?: return@forEach
//                symbolDefinitions[name] = Symbol(definition, type)
//            }
//            if (definition is TolkVar) {
//                ctx.setType(definition, type)
//            }
////            println("set $name = $type")
//        }
//
////        println("lhs: $varType")
////        println("rhs: $expressionType")
//    }
//
//    private fun infer(
//        element: TolkExpressionStatement
//    ) {
//        infer(element.expression)
//    }
//
//    private fun processExpression(element: TolkExpression, flow: TolkFlowContext, expectedType: TolkType? = null): Array<TolkFlowContext> {
//
//        return arrayOf(flow)
//    }
//
//    private fun infer(element: TolkExpression, expectedType: TolkType? = null): TolkType? {
//        ctx.expressionTypes[element]?.let {
//            return it
//        }
//        return when (element) {
//            is TolkBinExpression -> infer(element)
//            is TolkTernaryExpression -> infer(element, expectedType)
//            is TolkPrefixExpression -> infer(element)
//            is TolkDotExpression -> infer(element, expectedType)
//            is TolkSafeAccessExpression -> infer(element, expectedType)
//            is TolkNotNullExpression -> infer(element, expectedType)
//            is TolkCallExpression -> infer(element, expectedType)
//            is TolkTupleExpression -> infer(element, expectedType)
//            is TolkParenExpression -> infer(element, expectedType)
//            is TolkTensorExpression -> infer(element, expectedType)
//            is TolkReferenceExpression -> infer(element)
//            is TolkLiteralExpression -> element.type
//            is TolkUnitExpression -> TolkType.Unit
//            is TolkAsExpression -> infer(element)
//            else -> expectedType
//        }
//    }
//
//    private val boolOperators = tokenSetOf(
//        TolkElementTypes.ANDAND,
//        TolkElementTypes.OROR,
//    )
//    private val boolResultOperators = tokenSetOf(
//        TolkElementTypes.EQEQ,
//        TolkElementTypes.NEQ,
//        TolkElementTypes.SPACESHIP,
//        TolkElementTypes.LT,
//        TolkElementTypes.GT,
//        TolkElementTypes.LEQ,
//        TolkElementTypes.GEQ,
//        TolkElementTypes.ANDAND,
//        TolkElementTypes.OROR,
//    )
//
//    private fun infer(element: TolkBinExpression): TolkType? {
//        var expectedType: TolkType? = null
//        val operator = element.binaryOp.firstChild.elementType
//        if (operator in boolOperators) {
//            expectedType = TolkType.Bool
//        }
//        val rightType = element.right?.let { expression ->
//            infer(expression, expectedType)
//        }
//        val leftType = infer(element.left, expectedType)
//        if (rightType == null && leftType == null) {
//            return null
//        }
//        if (expectedType == TolkType.Bool) {
//            if (rightType != TolkType.Bool) {
//                ctx.addDiagnostic(
//                    TolkTypeMismatchDiagnostic(
//                        element.right ?: element,
//                        TolkType.Bool,
//                        rightType
//                    )
//                )
//            }
//            if (leftType != TolkType.Bool) {
//                ctx.addDiagnostic(
//                    TolkTypeMismatchDiagnostic(
//                        element.left,
//                        TolkType.Bool,
//                        leftType
//                    )
//                )
//            }
//        } else {
//            if (leftType != null && rightType != null && leftType != rightType) {
//                ctx.addDiagnostic(
//                    TolkTypeMismatchDiagnostic(
//                        element.right ?: element,
//                        leftType,
//                        rightType
//                    )
//                )
//            }
//        }
//        val resultType = if (operator in boolResultOperators) {
//            TolkType.Bool
//        } else {
//            leftType ?: rightType
//        }
//        return ctx.setType(element, resultType)
//    }
//
//    private fun infer(element: TolkTernaryExpression, expectedType: TolkType?): TolkType? {
//        infer(element.condition, TolkType.Bool)
//        val thenType = element.thenBranch?.let { branch ->
//            infer(branch, expectedType)
//        }
//        val elseType = element.elseBranch?.let { branch ->
//            infer(branch, expectedType)
//        }
//        return ctx.setType(element, thenType ?: elseType ?: expectedType)
//    }
//
//    private fun infer(element: TolkPrefixExpression): TolkType? {
//        val type = element.type
//        element.expression?.let { expression ->
//            infer(expression, type)
//        }
//        return type
//    }
//
//    private fun infer(
//        element: TolkDotExpression,
//        expectedType: TolkType? = null,
//    ): TolkType? {
//        infer(element.left)
//        val type = element.right?.let { expression ->
//            if (expression is TolkCallExpression) {
//                infer(expression, expectedType, withFirstArg = element.left)
//            } else {
//                infer(expression, expectedType)
//            }
//        }
//        return ctx.setType(element, type ?: expectedType)
//    }
//
//    private fun infer(
//        element: TolkSafeAccessExpression,
//        expectedType: TolkType? = null,
//    ): TolkType? {
//        val left = element.left
//        val leftType = infer(left)?.let {
//            if (it is TolkType.TolkUnionType) it.removeNullability() else it
//        }
//        ctx.setType(left, leftType)
//        val type = element.right?.let { expression ->
//            if (expression is TolkCallExpression) {
//                infer(expression, expectedType, withFirstArg = left)
//            } else {
//                infer(expression, expectedType)
//            }
//        }
//        return ctx.setType(element, type?.nullable() ?: expectedType)
//    }
//
//    private fun infer(
//        element: TolkCallExpression,
//        expectedType: TolkType? = null,
//        withFirstArg: TolkExpression? = null
//    ): TolkType? {
////        println("start infer ${element.text} expected: $expectedType")
//        val expression = element.expression
//        infer(expression)
//        val functionType = ctx.getType(expression) as? TolkType.Function
//        val parameterTypes = functionType?.inputType?.let { param ->
//            when (param) {
//                is TolkType.Tensor -> param.elements
//                TolkType.Unit -> emptyList()
//                else -> listOf(param)
//            }
//        }
////        println("function type: $functionType")
//        val arguments = ArrayList<TolkExpression>()
//        if (withFirstArg != null) {
//            arguments.add(withFirstArg)
//        }
//        element.argumentList.argumentList.forEach {
//            arguments.add(it.expression)
//        }
//        // can't resolve expected types, just infer without context
//        if (parameterTypes == null || parameterTypes.size != arguments.size) {
////            println("params(${parameterTypes?.size}) != args(${arguments.size})")
//            arguments.forEach {
//                infer(it)
//            }
//            return ctx.setType(element, expectedType ?: functionType?.returnType)
//        }
////        println("parameterTypes: $parameterTypes")
//
//        // we can provide expected types for generic resolve
//        val typeMapping = HashMap<TolkTypeParameter, TolkType>()
//        fun unify(paramType: TolkType, argType: TolkType?) {
//            when {
//                paramType is TolkType.Function && argType is TolkType.Function -> {
//                    unify(paramType.inputType, argType.inputType)
//                    unify(paramType.returnType, argType.returnType)
//                }
//
//                paramType is TolkType.TypedTuple && argType is TolkType.TypedTuple -> {
//                    paramType.elements.zip(argType.elements).forEach { (param, arg) ->
//                        unify(param, arg)
//                    }
//                }
//
//                paramType is TolkType.Tensor && argType is TolkType.Tensor -> {
//                    paramType.elements.zip(argType.elements).forEach { (param, arg) ->
//                        unify(param, arg)
//                    }
//                }
//
//                paramType is TolkType.ParameterType && argType != null && argType !is TolkType.ParameterType -> {
//                    typeMapping[paramType.psiElement] = argType
//                }
//            }
//        }
//
//        parameterTypes.zip(arguments).forEach { (paramType, arg) ->
//            val argType = infer(arg, expectedType = paramType)
//            unify(paramType, argType)
//        }
//
//        var returnType = functionType.returnType.substitute(typeMapping)
//        if (returnType is TolkType.ParameterType) {
//            returnType = expectedType ?: returnType
//        }
////        println("end infer ${element.text} = $returnType")
//        return ctx.setType(element, returnType)
//    }
//
//    private fun infer(element: TolkParenExpression, expectedType: TolkType? = null): TolkType? {
//        return ctx.setType(element, element.expression?.let { expression ->
//            infer(expression, expectedType)
//        } ?: expectedType)
//    }
//
//    private fun infer(element: TolkTensorExpression, expectedType: TolkType? = null): TolkType? {
//        val expectedTypes = (expectedType as? TolkType.Tensor)?.elements
//        val actualTypes = element.expressionList.mapIndexedNotNull { index, expression ->
//            val expectedType = expectedTypes?.getOrNull(index)
//            if (expectedType is TolkType.Unknown) {
//                infer(expression)
//            } else {
//                infer(expression, expectedType)
//            }
//        }
//
//        return ctx.setType(
//            element, if (expectedTypes == null || expectedTypes.size == actualTypes.size) {
//                TolkType.Tensor(actualTypes)
//            } else {
//                expectedType
//            }
//        )
//    }
//
//    private fun infer(element: TolkTupleExpression, expectedType: TolkType? = null): TolkType? {
//        val expectedTypes = (expectedType as? TolkType.TypedTuple)?.elements
//        val expressions = element.expressionList
//        val actualTypes = expressions.mapIndexedNotNull { index, expression ->
//            val expectedType = expectedTypes?.getOrNull(index)
//            if (expectedType is TolkType.Unknown) {
//                infer(expression)
//            } else {
//                infer(expression, expectedType)
//            }
//        }
//        return ctx.setType(
//            element,
//            if (expectedTypes == null || expectedTypes.size == actualTypes.size) {
//                TolkType.TypedTuple(actualTypes)
//            } else {
//                expectedType
//            }
//        )
//    }
//
//    private fun infer(element: TolkReferenceExpression): TolkType? {
//        val name = element.name?.removeSurrounding("`") ?: return null
//        val found = resolveSymbol(name) ?: return null
//        val foundElement = found.element
//        ctx.setResolvedRefs(element, listOf(PsiElementResolveResult(found.element)))
//        var type = found.type
//        if (type == null) {
//            type = (foundElement as? TolkTypedElement)?.type ?: return null
//        }
//        if (foundElement is TolkVar) {
//            ctx.setType(found.element, type)
//        } else if (foundElement is TolkTypeParameterListOwner) {
//            val typeArgumentList = element.typeArgumentList?.typeExpressionList
//            val typeParameters = foundElement.typeParameterList?.typeParameterList
//            if (typeArgumentList != null && typeParameters != null) {
//                val typeMap = HashMap<TolkTypeParameter, TolkType>()
//                typeParameters.zip(typeArgumentList).forEach { (param, arg) ->
//                    val type = arg.type ?: return@forEach
//                    typeMap[param] = type
//                }
//                type = type.substitute(typeMap)
//            }
//        }
//        return ctx.setType(element, type)
//    }
//
//    private fun infer(element: TolkAsExpression): TolkType? {
//        infer(element.expression)
//        return element.type
//    }
//
//    private fun infer(element: TolkNotNullExpression, expectedType: TolkType? = null): TolkType? {
//        val type = infer(element.expression, expectedType?.removeNullability())?.removeNullability()
//        return ctx.setType(element, type ?: expectedType)
//    }
//
//    private fun resolveSymbol(name: String): Symbol? {
//        var scope: TolkInferenceWalker? = this
//        var found: Symbol? = null
//        while (found == null && scope != null) {
//            found = scope.symbolDefinitions[name]
//            scope = scope.parent
//        }
//        return found
//    }
}


enum class TolkUnreachableKind {
    Unknown,
    CantHappen,
    ThrowStatement,
    ReturnStatement,
    CallNeverReturnFunction,
    InfiniteLoop,
}

class TolkFlowContext(
    val globalSymbols: MutableMap<String, TolkNamedElement> = HashMap<String, TolkNamedElement>(),
    val types: MutableMap<TolkTypedElement, TolkType> = LinkedHashMap(),
    val symbols: MutableMap<String, TolkNamedElement> = LinkedHashMap(),
    var unreachable: TolkUnreachableKind? = null
) {
    constructor(other: TolkFlowContext) : this(
        other.globalSymbols,
        LinkedHashMap(other.types),
        LinkedHashMap(other.symbols),
        other.unreachable
    )

    fun getType(element: TolkTypedElement): TolkType? {
        return types[element]
    }

    fun getSymbol(
        project: Project,
        name: String?,
    ): TolkNamedElement? {
        val fullName = name?.removeSurrounding("`") ?: return null
        return symbols[fullName] ?: globalSymbols[fullName]
    }

    fun <T : TolkType> setType(element: TolkTypedElement, type: T?): T? {
        if (type != null) {
            types[element] = type
        }
        if (element is TolkNamedElement && element !is TolkReferenceExpression) {
            val name = element.name?.removeSurrounding("`") ?: return type
            symbols[name] = element
        }
        return type
    }

    fun merge(other: TolkFlowContext) = apply {
        other.types.forEach { k,v ->
            types.putIfAbsent(k, v)
        }
        other.symbols.forEach { k,v ->
            symbols.putIfAbsent(k, v)
        }
        unreachable = unreachable ?: other.unreachable
    }

    fun join(other: TolkFlowContext): TolkFlowContext {
        if (other.unreachable == TolkUnreachableKind.CantHappen) {
            return TolkFlowContext(globalSymbols, types, symbols, unreachable)
        }
        if (unreachable == TolkUnreachableKind.CantHappen) {
            return TolkFlowContext(other.globalSymbols, other.types, other.symbols, other.unreachable)
        }
        val joinedSymbols = LinkedHashMap(types)
        val joinedNames = LinkedHashMap(symbols)
        other.types.forEach { (element, type) ->
            joinedSymbols[element] = joinedSymbols[element]?.join(type) ?: type
        }
        other.symbols.forEach { (name, element) ->
            joinedNames[name] = element
        }
        val joinedUnreachable =
            if (unreachable != null && other.unreachable != null) TolkUnreachableKind.Unknown else null
        return TolkFlowContext(globalSymbols, joinedSymbols, joinedNames, joinedUnreachable)
    }
}

class TolkExpressionFlowContext(
    val outFlow: TolkFlowContext,
    val trueFlow: TolkFlowContext,
    val falseFlow: TolkFlowContext
) {
    constructor(outFlow: TolkFlowContext, cloneFlowForCondition: Boolean) : this(
        outFlow,
        if (cloneFlowForCondition) TolkFlowContext(outFlow) else outFlow,
        if (cloneFlowForCondition) TolkFlowContext(outFlow) else outFlow
    )
}
