package org.ton.intellij.tolk.type

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findPsiFile
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.util.parentOfType
import com.intellij.util.containers.OrderedSet
import org.ton.intellij.tolk.diagnostics.TolkDiagnostic
import org.ton.intellij.tolk.psi.*
import org.ton.intellij.tolk.psi.impl.*
import org.ton.intellij.util.recursionGuard
import org.ton.intellij.util.tokenSetOf
import java.math.BigInteger
import java.util.*

val TolkElement.inference: TolkInferenceResult?
    get() {
        val context: TolkInferenceContextOwner? =
            this as? TolkInferenceContextOwner ?: context?.parentOfType<TolkInferenceContextOwner>(withSelf = true)
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
    private val constTypes: Map<TolkConstVar, TolkType>,
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
        } else if (element is TolkConstVar) {
            return constTypes[element]
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
    private val constTypes = HashMap<TolkConstVar, TolkType>()
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
        } else if (element is TolkConstVar) {
            return constTypes[element]
        }
        return expressionTypes[element]
    }

    fun <T : TolkType> setType(element: TolkVarDefinition, type: T?): T? {
        if (type != null) {
            varTypes[element] = type
        }
        return type
    }

    fun <T : TolkType> setType(element: TolkConstVar, type: T?): T? {
        if (type != null) {
            constTypes[element] = type
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
                unreachable = flow.unreachable
            }
            is TolkFile -> {
                val walker = TolkInferenceWalker(this)
                var flow = TolkFlowContext()

                val commonStdlib =
                    TolkIncludeDefinitionMixin.resolveTolkImport(element.project, element, "@stdlib/common")
                if (commonStdlib != null) {
                    val tolkCommonStdlib = commonStdlib.findPsiFile(element.project) as? TolkFile
                    if (tolkCommonStdlib != null && element != tolkCommonStdlib) {
                        flow = walker.inferFile(tolkCommonStdlib, flow, false)
                    }
                }

                walker.inferFile(element, flow)
            }
        }

        return TolkInferenceResult(resolvedRefs, expressionTypes, varTypes, constTypes, returnStatements, unreachable)
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
        element.constVars.forEach { constVar ->
            nextFlow = inferConstant(constVar, nextFlow)
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
            nextFlow.setSymbol(typeParameter, typeParameter.type ?: TolkType.Unknown)
        }
        element.parameterList?.parameterList?.forEach { functionParameter ->
            nextFlow.setSymbol(functionParameter, functionParameter.type ?: TolkType.Unknown)
        }
        element.functionBody?.blockStatement?.let {
            nextFlow = processBlockStatement(it, nextFlow)
        }
        return nextFlow
    }

    fun inferConstant(element: TolkConstVar, flow: TolkFlowContext): TolkFlowContext {
        val expression = element.expression
        if (expression != null) {
            val typeHint = element.typeExpression?.type
            inferExpression(expression, flow, false, typeHint).outFlow
            val exprType = ctx.getType(expression)
            ctx.setType(element, typeHint ?: exprType)
        }
        return flow
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
        } ?: afterCondition.trueFlow
        val falseFlow = element.elseBranch?.statement?.let {
            inferStatement(it, afterCondition.falseFlow)
        } ?: afterCondition.falseFlow

        return trueFlow.join(falseFlow)
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
        processBlockStatement(body, afterCond2.trueFlow)

        return afterCond2.falseFlow
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
            element.throwStatement?.let {
                inferStatement(it, afterCond.falseFlow)
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

        catchExpr.getOrNull(0)?.let { catchFlow.setSymbol(it, TolkType.Int) }
        catchExpr.getOrNull(1)?.let { catchFlow.setSymbol(it, TolkType.Unknown) }
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
        val varDefinitionType = ctx.getType(varDefinition)
        val expression = element.expression ?: return nextFlow
        nextFlow = inferExpression(expression, nextFlow, false, varDefinitionType).outFlow
        processVarDefinitionAfterRight(varDefinition, ctx.getType(expression) ?: TolkType.Unknown, nextFlow)

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
                    val tensorElementType = ctx.getType(tensorElement) ?: TolkType.Unknown
                    tensorElementTypes.add(tensorElementType)
                }
                val tensorType = TolkType.tensor(tensorElementTypes)
                ctx.setType(element, tensorType)
            }

            is TolkVarTuple -> {
                val tupleElements = element.varDefinitionList
                val tupleElementTypes = ArrayList<TolkType>(tupleElements.size)
                for (tupleElement in tupleElements) {
                    nextFlow = processVarDefinition(tupleElement, nextFlow)
                    val tupleElementType = ctx.getType(tupleElement) ?: TolkType.Unknown
                    tupleElementTypes.add(tupleElementType)
                }
                val tupleType = TolkType.typedTuple(tupleElementTypes)
                ctx.setType(element, tupleType)
            }

            is TolkVarRedef -> {
                nextFlow = inferExpression(element.referenceExpression, nextFlow, false).outFlow
                val type = ctx.getType(element.referenceExpression)
                ctx.setType(element, type)
            }

            is TolkVar -> {
                val typeHint = element.typeExpression?.type ?: TolkType.Unknown
                ctx.setType(element, typeHint)
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
                ctx.setType(element, rightType)
                outFlow.setSymbol(element, rightType)
            }

            is TolkVarTensor -> {
                val rightElements = if (rightType is TolkTensorType) rightType.elements else listOf(rightType)
                val leftElements = element.varDefinitionList
                val typesList = ArrayList<TolkType>(rightElements.size)
                leftElements.forEachIndexed { index, expression ->
                    val ithRightType = rightElements.getOrNull(index) ?: TolkType.Unknown
                    processVarDefinitionAfterRight(expression, ithRightType, outFlow)
                    typesList.add(ctx.getType(expression) ?: TolkType.Unknown)
                }
                val tensorType = TolkType.tensor(typesList)
                ctx.setType(element, tensorType)
            }

            is TolkVarTuple -> {
                val rightElements = if (rightType is TolkTypedTupleType) rightType.elements else listOf(rightType)
                val leftElements = element.varDefinitionList
                val typesList = ArrayList<TolkType>(rightElements.size)
                leftElements.forEachIndexed { index, expression ->
                    val ithRightType = rightElements.getOrNull(index) ?: TolkType.Unknown
                    processVarDefinitionAfterRight(expression, ithRightType, outFlow)
                    typesList.add(ctx.getType(expression) ?: TolkType.Unknown)
                }
                val tupleType = TolkType.typedTuple(typesList)
                ctx.setType(element, tupleType)
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
            is TolkCallExpression -> inferCallExpressionNew(element, flow, usedAsCondition, hint)
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
        ctx.setType(element, element.type)
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

        val conditionType = ctx.getType(condition)
        if (conditionType == TolkType.TRUE) {
            ctx.setType(
                element,
                ctx.getType(thenBranch)
            )
            return afterTrue
        }

        if (conditionType == TolkType.FALSE) {
            ctx.setType(
                element,
                ctx.getType(elseBranch)
            )
            return afterFalse
        }

        val thenType = ctx.getType(thenBranch) ?: TolkType.Unknown
        val elseType = ctx.getType(elseBranch) ?: TolkType.Unknown
        val resultType = thenType.join(elseType)

        val outFlow = afterTrue.outFlow.join(afterFalse.outFlow)
        ctx.setType(element, resultType)

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
                ctx.setType(element, TolkType.Bool)
            }

            in AND_OR_XOR_OPERATORS -> {
                nextFlow = inferExpression(left, nextFlow, true).outFlow
                if (right != null) {
                    nextFlow = inferExpression(right, nextFlow, true).outFlow
                }
                val leftType = ctx.getType(left) ?: TolkType.Unknown
                val rightType = right?.let { ctx.getType(right) } ?: TolkType.Unknown

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

                ctx.setType(element, elementType)
            }

            TolkElementTypes.ANDAND -> {
                ctx.setType(element, TolkType.Bool)
                val afterLeft = inferExpression(left, nextFlow, true)
                if (right == null) {
                    return afterLeft.outFlow.toResult()
                }
                val afterRight = inferExpression(right, afterLeft.trueFlow, true)
                if (!usedAsCondition) {
                    val outFlow = afterLeft.falseFlow.join(afterRight.outFlow)
                    val leftType = ctx.getType(left) ?: TolkType.Unknown
                    val rightType = ctx.getType(right) ?: TolkType.Unknown
                    val elementType = leftType.join(rightType)
                    ctx.setType(element, elementType)
                    return TolkExpressionFlowContext(outFlow, false)
                }
                val outFlow = afterLeft.outFlow.join(afterRight.outFlow)
                val leftType = ctx.getType(left) ?: TolkType.Unknown
                val rightType = ctx.getType(right) ?: TolkType.Unknown
                val trueFlow = afterRight.trueFlow
                val falseFlow = afterLeft.falseFlow.join(afterRight.falseFlow)
                val elementType = leftType.join(rightType)
                ctx.setType(element, elementType)
                return TolkExpressionFlowContext(outFlow, trueFlow, falseFlow)
            }

            TolkElementTypes.OROR -> {
                ctx.setType(element, TolkType.Bool)
                val afterLeft = inferExpression(left, nextFlow, true)
                if (right == null) {
                    return afterLeft.outFlow.toResult()
                }
                val afterRight = inferExpression(right, afterLeft.falseFlow, true)
                if (!usedAsCondition) {
                    val outFlow = afterLeft.trueFlow.join(afterRight.outFlow)
                    val leftType = ctx.getType(left) ?: TolkType.Unknown
                    val rightType = ctx.getType(right) ?: TolkType.Unknown
                    val elementType = leftType.join(rightType)
                    ctx.setType(element, elementType)
                    return TolkExpressionFlowContext(outFlow, false)
                }
                val outFlow = afterLeft.outFlow.join(afterRight.outFlow)
                val trueFlow = afterLeft.trueFlow.join(afterRight.trueFlow)
                val falseFlow = afterRight.falseFlow
                val leftType = ctx.getType(left) ?: TolkType.Unknown
                val rightType = ctx.getType(right) ?: TolkType.Unknown
                val elementType = leftType.join(rightType)
                ctx.setType(element, elementType)

                return TolkExpressionFlowContext(outFlow, trueFlow, falseFlow)
            }

            TolkElementTypes.EQ -> {
                nextFlow = inferExpression(left, nextFlow, false).outFlow
                if (right == null) {
                    return nextFlow.toResult()
                }
                nextFlow = inferExpression(right, nextFlow, false).outFlow
                val rightType = ctx.getType(right) ?: TolkType.Unknown
                processAssigmentAfterRight(left, rightType, nextFlow)
                ctx.setType(element, rightType)
            }

            in ASIGMENT_OPERATORS -> {
                nextFlow = inferExpression(left, nextFlow, false).outFlow
                if (right == null) {
                    return nextFlow.toResult()
                }
                nextFlow = inferExpression(right, nextFlow, false).outFlow
                val leftType = ctx.getType(left) ?: TolkType.Unknown
                val rightType = ctx.getType(right) ?: TolkType.Unknown
                if (TolkType.Int.isSuperType(leftType) && TolkType.Int.isSuperType(rightType)) {
                    ctx.setType(element, TolkType.Int)
                    val symbol = extractSinkExpression(left)
                    if (symbol != null) {
                        nextFlow.setSymbol(symbol, TolkType.Int)
                    }
                }
            }

            else -> {
                nextFlow = inferExpression(left, nextFlow, false).outFlow
                if (right == null) {
                    ctx.setType(element, TolkType.Int)
                    return nextFlow.toResult()
                }
                nextFlow = inferExpression(right, nextFlow, false).outFlow
                val leftType = ctx.getType(left)
                val rightType = ctx.getType(right)

                if (leftType != null && rightType != null) {
                    ctx.setType(element, leftType.join(rightType))
                } else {
                    ctx.setType(element, leftType ?: rightType)
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

        val exprType = ctx.getType(expression) ?: return afterExpr
        val notNullType = exprType.removeNullability()
        val resultType = if (exprType == TolkType.Null) { // `expr == null` is always true
            if (isInverted) TolkType.FALSE else TolkType.TRUE
        } else if (notNullType == TolkType.Never || notNullType == exprType) { // `expr == null` is always false
            if (isInverted) TolkType.TRUE else TolkType.FALSE
        } else {
            TolkType.Bool
        }
        ctx.setType(element, resultType)

        if (!usedAsCondition) {
            return afterExpr
        }

        val trueFlow = TolkFlowContext(afterExpr.outFlow)
        val falseFlow = TolkFlowContext(afterExpr.outFlow)
        val sExpr = extractSinkExpression(expression) ?: return afterExpr
        if (resultType == TolkType.TRUE) {
            falseFlow.unreachable = TolkUnreachableKind.CantHappen
            falseFlow.setSymbol(sExpr, TolkType.Never)
        } else if (resultType == TolkType.FALSE) {
            trueFlow.unreachable = TolkUnreachableKind.CantHappen
            trueFlow.setSymbol(sExpr, TolkType.Never)
        } else if (!isInverted) {
            trueFlow.setSymbol(sExpr, TolkType.Null)
            falseFlow.setSymbol(sExpr, notNullType)
        } else {
            trueFlow.setSymbol(sExpr, notNullType)
            falseFlow.setSymbol(sExpr, TolkType.Null)
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
        val leftType = ctx.getType(left)
        val joinType = leftType?.join(rightType) ?: rightType

        when (left) {
            is TolkReferenceExpression -> {
                val resolvedElement = extractSinkExpression(left)
                if (resolvedElement != null) {
                    flow.setSymbol(resolvedElement, rightType)
                }
            }

            is TolkTensorExpression -> {
                if (joinType is TolkTensorType) {
                    left.expressionList.zip(joinType.elements).forEach { (left, right) ->
                        processAssigmentAfterRight(left, right, flow)
                    }
                }
            }

            is TolkTupleExpression -> {
                if (joinType is TolkTypedTupleType) {
                    left.expressionList.zip(joinType.elements).forEach { (left, right) ->
                        processAssigmentAfterRight(left, right, flow)
                    }
                }
            }

            is TolkParenExpression -> {
                processAssigmentAfterRight(left.expression ?: return, rightType, flow)
            }

            else -> {
                val sExpr = extractSinkExpression(left) ?: return
                flow.setSymbol(sExpr, rightType)
            }
        }
    }

    private fun inferReferenceExpression(
        element: TolkReferenceExpression,
        flow: TolkFlowContext,
        usedAsCondition: Boolean
    ): TolkExpressionFlowContext {
        val name = element.name
        val symbol = flow.getSymbol(name)
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

                is TolkGlobalVar -> {
                    if (ctx.owner is TolkFunction) {
                        symbol.type
                    } else {
                        ctx.getType(symbol)
                    }
                }
                is TolkConstVar -> if (ctx.owner is TolkFunction) {
                    symbol.type
                } else {
                    ctx.getType(symbol)
                }
                else -> flow.getType(symbol)
            }
            ctx.setType(element, type)
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
            val type = ctx.getType(item) ?: TolkType.Unknown
            typesList.add(type)
        }
        val tupleType = TolkType.typedTuple(typesList)
        ctx.setType(element, tupleType)
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
            val type = ctx.getType(item) ?: TolkType.Unknown
            typesList.add(type)
        }
        val tensorType = TolkType.tensor(typesList)
        ctx.setType(element, tensorType)
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
        val functionType = ctx.getType(callee)

        val argumentTypes = ArrayList<TolkType>(element.argumentList.argumentList.size + 1)

        var functionSymbol = if (callee is TolkReferenceExpression) {
            val resolvedRefs = ctx.getResolvedRefs(callee)
            resolvedRefs.firstOrNull()?.element as? TolkFunction
        } else null
        var deltaParams = 0
        if (callee is TolkDotExpression) {
            val firstArgType = ctx.getType(callee.left) ?: TolkType.Unknown
            argumentTypes.add(firstArgType)
            deltaParams = 1
            val calleeRight = callee.right
            if (calleeRight is TolkReferenceExpression) {
                functionSymbol = ctx.getResolvedRefs(calleeRight).firstOrNull() as? TolkFunction
            }
        }

        element.argumentList.argumentList.forEachIndexed { index, argument ->
            val argExpr = argument.expression
            nextFlow = inferExpression(argExpr, nextFlow, false).outFlow
            val argType = ctx.getType(argExpr) ?: TolkType.Unknown
            argumentTypes.add(argType)

            val param =
                functionSymbol?.parameterList?.parameterList?.getOrNull(deltaParams + index) ?: return@forEachIndexed
            val paramType = param.typeExpression?.type
            if (param.isMutable && paramType != null && paramType != argType) {
                val sExpr = extractSinkExpression(argExpr)
                if (sExpr != null) {
                    nextFlow.setSymbol(sExpr, paramType)
                }
            }
        }

        val callType = TolkFunctionType(TolkType.tensor(argumentTypes), hint ?: TolkType.Unknown)

        if (functionType != null) {
            val resolvedFunctionType = (functionType as? TolkFunctionType)?.resolveGenerics(callType)
            ctx.setType(element, resolvedFunctionType?.returnType)
        } else {
            ctx.setType(element, callType.returnType)
        }

        return TolkExpressionFlowContext(nextFlow, usedAsCondition)
    }

    private fun inferCallExpressionNew(
        element: TolkCallExpression,
        flow: TolkFlowContext,
        usedAsCondition: Boolean,
        hint: TolkType?
    ): TolkExpressionFlowContext {
        var nextFlow = flow
        val callee = element.expression
        var deltaParams = 0
        var functionSymbol: TolkFunction? = null
        var typeArgs: List<TolkType>? = null

        nextFlow = inferExpression(callee, nextFlow, false).outFlow

        if (callee is TolkReferenceExpression) { // `globalF()` / `globalF<int>()` / `local_var()` / `SOME_CONST()`
            functionSymbol = ctx.getResolvedRefs(callee).firstOrNull()?.element as? TolkFunction
            typeArgs = callee.typeArgumentList?.typeExpressionList?.map { it.type ?: TolkType.Unknown }
        } else if (callee is TolkDotExpression) { // `obj.someMethod()` / `obj.someMethod<int>()` / `getF().someMethod()` / `obj.SOME_CONST()`
            deltaParams = 1
            val calleeRight = callee.right
            if (calleeRight is TolkReferenceExpression) {
                functionSymbol = ctx.getResolvedRefs(calleeRight).firstOrNull()?.element as? TolkFunction
                typeArgs = calleeRight.typeArgumentList?.typeExpressionList?.map { it.type ?: TolkType.Unknown }

            }
        }

        // handle `local_var()` / `getF()()` / `5()` / `SOME_CONST()` / `obj.method()()()` / `tensorVar.0()`
        if (functionSymbol == null) {
            val callableFunction = ctx.getType(callee)
            if (callableFunction !is TolkFunctionType) {
                // TODO: inspection for 'calling a non-function'
                element.argumentList.argumentList.forEachIndexed { index, argument ->
                    val argExpr = argument.expression
                    nextFlow = inferExpression(argExpr, nextFlow, false).outFlow
                }
                return TolkExpressionFlowContext(nextFlow, usedAsCondition)
            }
            val parameters = callableFunction.parameters
            element.argumentList.argumentList.forEachIndexed { index, argument ->
                val argExpr = argument.expression
                nextFlow = inferExpression(argExpr, nextFlow, false, parameters.getOrNull(index)).outFlow
            }
            ctx.setType(element, callableFunction.returnType)
            return TolkExpressionFlowContext(nextFlow, usedAsCondition)
        }

        val arguments = element.argumentList.argumentList
        val parameters = functionSymbol.parameters

        val argumentTypes = ArrayList<TolkType>(arguments.size + deltaParams)

        if (callee is TolkDotExpression) {
            val firstArgType = ctx.getType(callee.left) ?: TolkType.Unknown
            argumentTypes.add(firstArgType)
        }

        arguments.forEachIndexed { index, argument ->
            val argExpr = argument.expression
            nextFlow = inferExpression(argExpr, nextFlow, false).outFlow
            val argType = calcDeclaredTypeBeforeSmartCast(argExpr) ?: ctx.getType(argExpr) ?: TolkType.Unknown
            argumentTypes.add(argType)
        }

        val callType = TolkFunctionType(TolkType.tensor(argumentTypes), hint ?: TolkType.Unknown)
        val resolvedFunctionType = functionSymbol.resolveGenerics(callType, typeArgs)
        val resolvedParameterTypes = resolvedFunctionType.parameters

        arguments.forEachIndexed { index, argument ->
            val parameter = parameters.getOrNull(index + deltaParams) ?: return@forEachIndexed
            val parameterType = resolvedParameterTypes.getOrNull(index) ?: return@forEachIndexed
            val argumentType = argumentTypes.getOrNull(index + deltaParams) ?: return@forEachIndexed
            if (parameter.isMutable && parameterType != argumentType) {
                val sExpr = extractSinkExpression(argument.expression)
                if (sExpr != null) {
                    nextFlow.setSymbol(sExpr, parameterType)
                }
            }
        }

        ctx.setType(element, resolvedFunctionType.returnType)
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
        val leftType = ctx.getType(left)

        val right = element.right

        if (right is TolkReferenceExpression) {
            nextFlow = inferExpression(right, nextFlow, false).outFlow
            ctx.setType(element, ctx.getType(right))
            return TolkExpressionFlowContext(nextFlow, usedAsCondition)
        }
        val exprFlow = TolkExpressionFlowContext(nextFlow, usedAsCondition)
        val index = element.targetIndex ?: return exprFlow

        when (leftType) {
            is TolkTensorType -> {
                if (index >= leftType.elements.size) {
                    // todo add diagnostic: invalid tensor index, expected 0..leftType.elements.size-1
                    return exprFlow
                }
                var type = leftType.elements[index]
                extractSinkExpression(element)?.let { sExpr ->
                    flow.getType(sExpr)?.let { sType ->
                        type = sType
                    }
                }
                ctx.setType(element, type)
                right?.let { right ->
                    ctx.setType(right, type)
                }
                return exprFlow
            }

            is TolkTypedTupleType -> {
                if (index >= leftType.elements.size) {
                    // todo add diagnostic: invalid tuple index, expected 0..leftType.elements.size-1
                    return exprFlow
                }
                var type = leftType.elements[index]
                extractSinkExpression(element)?.let { sExpr ->
                    flow.getType(sExpr)?.let { sType ->
                        type = sType
                    }
                }
                ctx.setType(element, type)
                right?.let { right ->
                    ctx.setType(right, type)
                }
                return exprFlow
            }

            is TolkTupleType -> {
                if (hint != null) {
                    ctx.setType(element, hint)
                }
                return exprFlow
            }

            else -> {
                return exprFlow
            }
        }
    }

    private fun inferParenExpression(
        element: TolkParenExpression,
        flow: TolkFlowContext,
        usedAsCondition: Boolean,
        hint: TolkType?
    ): TolkExpressionFlowContext {
        val expression = element.expression ?: return TolkExpressionFlowContext(flow, usedAsCondition)
        val afterExpr = inferExpression(expression, flow, usedAsCondition, hint)
        ctx.setType(element, ctx.getType(expression))
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
                val expressionType = ctx.getType(expression) as? TolkIntType ?: TolkType.Int
                val resultType = expressionType.negate()
                ctx.setType(element, resultType)
            }

            TolkElementTypes.EXCL -> {
                val expressionType = ctx.getType(expression) as? TolkBoolType ?: TolkType.Bool
                val resultType = expressionType.negate()
                ctx.setType(element, resultType)
                afterExpr = TolkExpressionFlowContext(afterExpr.outFlow, afterExpr.falseFlow, afterExpr.trueFlow)
            }

            in INT_PREFIX_OPERATORS -> {
                ctx.setType(element, TolkType.Int)
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
        val type = ctx.getType(expression)
        if (type != null && type.isNullable()) {
            ctx.setType(element, type.removeNullability())
        } else {
            ctx.setType(element, type)
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
        ctx.setType(element, asType)
        if (!usedAsCondition) {
            return afterExpr
        }

        return TolkExpressionFlowContext(afterExpr.outFlow, true)
    }

    private fun extractSinkExpression(expression: TolkExpression): TolkSinkExpression? {
        return when (expression) {
            is TolkReferenceExpression -> {
                val symbol = ctx.getResolvedRefs(expression).firstOrNull()?.element as? TolkSymbolElement ?: return null
                return TolkSinkExpression(symbol)
            }

            is TolkDotExpression -> {
                var currentDot: TolkDotExpression = expression
                var indexPath = 0L
                while (true) {
                    val targetIndex = currentDot.targetIndex ?: break
                    if (targetIndex !in 0..255) break
                    indexPath = (indexPath shl 8) + targetIndex + 1
                    currentDot = currentDot.left.unwrapNotNull() as? TolkDotExpression ?: break
                }
                val ref = currentDot.left.unwrapNotNull() as? TolkReferenceExpression ?: return null
                val symbol = ctx.getResolvedRefs(ref).firstOrNull()?.element as? TolkSymbolElement ?: return null
                return TolkSinkExpression(symbol, indexPath)
            }

            is TolkParenExpression -> extractSinkExpression(expression.unwrapParen())
            is TolkBinExpression -> extractSinkExpression(expression.left)
            else -> null
        }
    }

    private fun calcDeclaredTypeBeforeSmartCast(expression: TolkExpression): TolkType? {
        when (expression) {
            is TolkReferenceExpression -> {
                val symbol = ctx.getResolvedRefs(expression).firstOrNull()?.element as? TolkSymbolElement
                if (symbol is TolkVar) {
                    return ctx.getType(symbol)
                }
                return symbol?.type
            }
            is TolkDotExpression -> {
                val index = expression.targetIndex ?: return null
                val leftType = calcDeclaredTypeBeforeSmartCast(expression.left) ?: return null
                if (leftType is TolkTensorType) {
                    return leftType.elements.getOrNull(index)
                }
                if (leftType is TolkTypedTupleType) {
                    return leftType.elements.getOrNull(index)
                }
            }
            is TolkParenExpression -> {
                return calcDeclaredTypeBeforeSmartCast(expression.unwrapParen())
            }
        }
        return null
    }

    private fun TolkExpression.unwrapNotNull(): TolkExpression {
        var current = this
        while (current is TolkNotNullExpression) {
            current = current.expression
        }
        return current
    }

    private fun TolkExpression.unwrapParen(): TolkExpression {
        var current = this
        while (current is TolkParenExpression) {
            current = current.expression ?: return current
        }
        return current
    }
}
