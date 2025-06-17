package org.ton.intellij.tolk.type

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.util.parentOfType
import com.intellij.util.SmartList
import org.ton.intellij.tolk.codeInsight.hint.iterateOverParameters
import org.ton.intellij.tolk.diagnostics.TolkDiagnostic
import org.ton.intellij.tolk.psi.*
import org.ton.intellij.tolk.psi.impl.*
import org.ton.intellij.tolk.psi.reference.TolkTypeReference
import org.ton.intellij.tolk.psi.reference.collectFunctionCandidates
import org.ton.intellij.tolk.psi.reference.resolveFieldLookupReferenceWithReceiver
import org.ton.intellij.util.recursionGuard
import org.ton.intellij.util.tokenSetOf
import java.math.BigInteger
import java.util.*

val TolkElement.inference: TolkInferenceResult?
    get() {
        val contextOwner = parentOfType<TolkInferenceContextOwner>(true) ?: return null
        return contextOwner.selfInferenceResult
    }

fun inferTypesIn(element: TolkInferenceContextOwner): TolkInferenceResult {
    val ctx = TolkInferenceContext(element.project, element)
    return recursionGuard(element, memoize = false) {
        ctx.infer(element)
    } ?: throw CyclicReferenceException(element)
}

class CyclicReferenceException(val element: TolkInferenceContextOwner) :
    IllegalStateException("Can't do inference on cyclic inference context owner: $element")

interface TolkInferenceData {
    val returnStatements: List<TolkReturnStatement>

    fun getResolvedRefs(element: TolkElement): Collection<PsiElementResolveResult>

    fun getType(element: TolkTypedElement?): TolkTy?
}

private val EMPTY_RESOLVED_SET = emptySet<PsiElementResolveResult>()

data class TolkInferenceResult(
    private val resolvedRefs: Map<TolkElement, Collection<PsiElementResolveResult>>,
    private val expressionTypes: Map<TolkTypedElement, TolkTy>,
    private val varTypes: Map<TolkVarDefinition, TolkTy>,
    private val constTypes: Map<TolkConstVar, TolkTy>,
    private val resolvedFunctions: Map<TolkCallExpression, TolkFunction>,
    override val returnStatements: List<TolkReturnStatement>,
    val unreachable: TolkUnreachableKind? = null
) : TolkInferenceData {
    val timestamp = System.nanoTime()

    override fun getResolvedRefs(element: TolkElement): Collection<PsiElementResolveResult> {
        return resolvedRefs[element] ?: EMPTY_RESOLVED_SET
    }

    override fun getType(element: TolkTypedElement?): TolkTy? {
        if (element is TolkVarDefinition) {
            return varTypes[element]
        } else if (element is TolkConstVar) {
            return constTypes[element]
        }
        return expressionTypes[element ?: return null]
    }

    fun getResolvedFunction(expression: TolkCallExpression): TolkFunction? {
        return resolvedFunctions[expression]
    }

    companion object
}

class TolkInferenceContext(
    val project: Project,
    val owner: TolkInferenceContextOwner
) : TolkInferenceData {
    private val resolvedRefs = HashMap<TolkElement, Collection<PsiElementResolveResult>>()
    private val diagnostics = LinkedList<TolkDiagnostic>()
    private val varTypes = HashMap<TolkVarDefinition, TolkTy>()
    private val constTypes = HashMap<TolkConstVar, TolkTy>()
    private val resolvedFields: MutableMap<TolkFieldLookup, List<TolkElement>> = hashMapOf()
    private val resolvedFunctions: MutableMap<TolkCallExpression, TolkFunction> = hashMapOf()
    internal val expressionTypes = HashMap<TolkTypedElement, TolkTy>()
    override val returnStatements = LinkedList<TolkReturnStatement>()
    var declaredReturnType: TolkTy? = null

    override fun getResolvedRefs(element: TolkElement): Collection<PsiElementResolveResult> {
        return resolvedRefs[element] ?: EMPTY_RESOLVED_SET
    }

    fun getResolvedFields(element: TolkFieldLookup): List<TolkElement> {
        return resolvedRefs[element]?.mapNotNull { it.element as? TolkElement }.orEmpty()
    }

    fun setResolvedRefs(element: TolkReferenceElement, refs: Collection<PsiElementResolveResult>) {
        resolvedRefs[element] = refs
    }

    override fun getType(element: TolkTypedElement?): TolkTy? {
        if (element is TolkVarDefinition) {
            return varTypes[element]
        } else if (element is TolkConstVar) {
            return constTypes[element]
        }
        return expressionTypes[element]
    }

    fun <T : TolkTy> setType(element: TolkVarDefinition, type: T?): T? {
        if (type != null) {
            varTypes[element] = type
        }
        return type
    }

    fun <T : TolkTy> setType(element: TolkConstVar, type: T?): T? {
        if (type != null) {
            constTypes[element] = type
        }
        return type
    }

    fun <T : TolkTy> setType(element: TolkExpression, type: T?): T? {
        if (type != null) {
            expressionTypes[element] = type
        }
        return type
    }

    fun setResolvedFunctions(
        expression: TolkCallExpression,
        function: TolkFunction
    ) {
        resolvedFunctions[expression] = function
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
                var flow = TolkFlowContext()
                flow = walker.inferFunction(element, flow)
                unreachable = flow.unreachable
            }

            is TolkConstVar -> {
                val walker = TolkInferenceWalker(this)
                walker.inferConstant(element, TolkFlowContext())
            }

            is TolkStructField -> {
                val walker = TolkInferenceWalker(this)
                walker.inferField(element, TolkFlowContext())
            }

            is TolkParameterDefault -> {
                val walker = TolkInferenceWalker(this)
                walker.inferParameterDefault(element, TolkFlowContext())
            }
        }

        return TolkInferenceResult(
            resolvedRefs,
            expressionTypes,
            varTypes,
            constTypes,
            resolvedFunctions,
            returnStatements,
            unreachable
        )
    }
}

private typealias LocalSymbolsScopes = ArrayDeque<MutableMap<String, TolkLocalSymbolElement>>

private fun LocalSymbolsScopes.openScope() = add(HashMap()).also {
//    println("Opened new scope: ${this.size} scopes: ${toString()}")
}

private fun LocalSymbolsScopes.closeScope() = removeLast().also {
//    println("Closed scope: ${this.size} scopes left, ${toString()}")
}

private fun LocalSymbolsScopes.lookupSymbol(name: String?): TolkLocalSymbolElement? {
    if (name == null) return null
    val iterator = descendingIterator()
    while (iterator.hasNext()) {
        val scope = iterator.next()
        val symbol = scope[name]
        if (symbol != null) {
//            println("Found symbol: $symbol in scope: ${this.size} scopes: ${toString()}")
            return symbol
        }
    }
    return null
}

private fun LocalSymbolsScopes.addLocalSymbol(symbol: TolkLocalSymbolElement): Boolean {
    val currentScope = last()
    val result = currentScope.put(symbol.name ?: return false, symbol) != null
//    println("adding symbol: $symbol; ${this}")
    return result
}

private inline fun <T> LocalSymbolsScopes.useScope(block: LocalSymbolsScopes.() -> T): T {
    openScope()
    try {
        return block()
    } finally {
        closeScope()
    }
}

class AssignLocalSymbolsVisitor : TolkVisitor() {
    private val currentScope = LocalSymbolsScopes()
    val resolvedSymbols = HashMap<TolkReferenceElement, TolkLocalSymbolElement>()

    override fun visitElement(o: TolkElement) {
        o.acceptChildren(this)
    }

    override fun visitVar(o: TolkVar) {
        currentScope.addLocalSymbol(o)
    }

    override fun visitVarExpression(o: TolkVarExpression) {
        o.expression?.accept(this) // in this order, so that `var x = x` is invalid, "x" on the right unknown
        o.varDefinition?.accept(this)
    }

    override fun visitReferenceExpression(o: TolkReferenceExpression) {
        val symbol = currentScope.lookupSymbol(o.referenceName) ?: return
        resolvedSymbols[o] = symbol
    }

    override fun visitBlockStatement(o: TolkBlockStatement) {
        currentScope.useScope {
            o.acceptChildren(this@AssignLocalSymbolsVisitor)
        }
    }

    override fun visitDoStatement(o: TolkDoStatement) {
        currentScope.useScope {
            o.blockStatement?.acceptChildren(this@AssignLocalSymbolsVisitor)
            o.expression?.accept(this@AssignLocalSymbolsVisitor)
            // in 'while' condition it's ok to use variables declared inside do
        }
    }

    override fun visitMatchExpression(o: TolkMatchExpression) {
        currentScope.useScope {
            o.acceptChildren(this@AssignLocalSymbolsVisitor)
        }
    }

    override fun visitMatchArm(o: TolkMatchArm) {
        o.matchBody?.accept(this)
        o.matchPattern.accept(this)
    }

    override fun visitMatchPatternReference(o: TolkMatchPatternReference) {
        val symbol = currentScope.lookupSymbol(o.referenceName) ?: return
        resolvedSymbols[o] = symbol
    }

    override fun visitCatchParameter(o: TolkCatchParameter) {
        currentScope.addLocalSymbol(o)
    }

    override fun visitCatch(o: TolkCatch) {
        currentScope.useScope {
            o.acceptChildren(this@AssignLocalSymbolsVisitor)
        }
    }

    override fun visitParameterElement(o: TolkParameterElement) {
        currentScope.addLocalSymbol(o)
    }

    override fun visitFunction(o: TolkFunction) {
        currentScope.useScope {
            o.parameterList?.accept(this@AssignLocalSymbolsVisitor)
            o.functionBody?.blockStatement?.accept(this@AssignLocalSymbolsVisitor)
        }
    }
}

class TolkExpressionInferenceResult(
    val expression: TolkExpression,
    val context: TolkExpressionFlowContext,
    val inferredType: TolkTy? = null,
    val substitution: Substitution = EmptySubstitution
)

class TolkInferenceWalker(
    val ctx: TolkInferenceContext,
//    val parent: TolkInferenceWalker? = null,
//    val throwableElements: MutableList<TolkThrowStatement> = LinkedList(),
) {
    private val project = ctx.project
    private val importFiles = LinkedHashSet<VirtualFile>()
    private var currentFunction: TolkFunction? = null
    private var localSymbols: Map<TolkReferenceElement, TolkLocalSymbolElement> = emptyMap()

    fun inferFunction(element: TolkFunction, flow: TolkFlowContext): TolkFlowContext {
        currentFunction = element

        try {
            var nextFlow = flow
            val selfType = element.receiverTy
            ctx.declaredReturnType = element.returnType?.let {
                if (it.selfKeyword != null) {
                    selfType
                } else {
                    it.typeExpression?.type
                }
            }
            element.typeParameterList?.typeParameterList?.forEach { typeParameter ->
                nextFlow.setSymbol(typeParameter, typeParameter.type ?: TolkTy.Unknown)
            }
            AssignLocalSymbolsVisitor().run {
                visitFunction(element)
                localSymbols = resolvedSymbols
                localSymbols.forEach { (ref, symbol) ->
                    ctx.setResolvedRefs(ref, listOf(PsiElementResolveResult(symbol)))
                }
            }
            val parameterList = element.parameterList
            if (parameterList != null) {
                parameterList.selfParameter?.let { selfParameter ->
                    nextFlow.setSymbol(selfParameter, selfType)
                }
                parameterList.parameterList.forEach { functionParameter ->
                    nextFlow.setSymbol(functionParameter, functionParameter.type ?: TolkTy.Unknown)
                }
            }
            element.functionBody?.blockStatement?.let {
                nextFlow = processBlockStatement(it, nextFlow)
            }
            return nextFlow
        } finally {
            currentFunction = null
        }
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

    fun inferField(element: TolkStructField, flow: TolkFlowContext): TolkFlowContext {
        val expression = element.expression ?: return flow
        val typeHint = element.typeExpression?.type
        inferExpression(expression, flow, false, typeHint).outFlow
        return flow
    }

    fun inferParameterDefault(
        element: TolkParameterDefault,
        flow: TolkFlowContext
    ): TolkFlowContext {
        val expression = element.expression ?: return flow
        val typeHint = element.parentOfType<TolkParameter>()!!.typeExpression.type
        inferExpression(expression, flow, false, typeHint).outFlow
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
            is TolkMatchStatement -> processMatchStatement(element, flow)
            else -> flow
        }
    }

    private fun processBlockStatement(element: TolkBlockStatement, flow: TolkFlowContext): TolkFlowContext {
        val statements = element.statementList
        if (statements.isEmpty()) return flow
        var nextFlow = flow
        for (statement in statements) {
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
        // do while is also handled twice; read comments above
        val loopEntryFacts = TolkFlowContext(flow)
        var nextFlow = processBlockStatement(body, flow)
        val condition = element.expression ?: return nextFlow
        val afterCond = inferExpression(condition, nextFlow, true)
        // second time
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

        catchExpr.getOrNull(0)?.let { catchFlow.setSymbol(it, TolkTy.Int) }
        catchExpr.getOrNull(1)?.let { catchFlow.setSymbol(it, TolkTy.Unknown) }
        val catchEnd = processBlockStatement(catchBody, catchFlow)

        return tryEnd.join(catchEnd)
    }

    private fun processExpressionStatement(
        element: TolkExpressionStatement,
        flow: TolkFlowContext
    ): TolkFlowContext {
        val nextFlow = inferExpression(element.expression, flow, false).outFlow
        return nextFlow
    }

    private fun processMatchStatement(element: TolkMatchStatement, flow: TolkFlowContext): TolkFlowContext {
        val nextFlow = inferExpression(element.matchExpression, flow, false).outFlow
        return nextFlow
    }

    private fun processVarExpression(element: TolkVarExpression, flow: TolkFlowContext): TolkExpressionFlowContext {
        val lhs = element.varDefinition ?: return TolkExpressionFlowContext(flow, false)
        val nextFlow = inferLeftSideVarAssigment(lhs, flow)
        val rhs = element.expression ?: return TolkExpressionFlowContext(nextFlow, false)
        val varDefinitionType = ctx.getType(lhs)
        val nextExprFlow = inferExpression(rhs, nextFlow, false, varDefinitionType)
        val exprType = ctx.getType(rhs) ?: TolkTy.Unknown
        processVarDefinitionAfterRight(lhs, exprType, nextExprFlow.outFlow)
        ctx.setType(element, exprType)
        return nextExprFlow
    }

    private fun inferAssigment(
        element: TolkBinExpression,
        flow: TolkFlowContext,
        usedAsCondition: Boolean
    ): TolkExpressionFlowContext {
        val lhs = element.left
        var nextFlow = inferLeftSideAssigment(lhs, flow)
        val rhs = element.right
        if (rhs != null) {
            val leftType = ctx.getType(lhs)
            nextFlow = inferExpression(rhs, nextFlow, false, leftType).outFlow
            val rightType = ctx.getType(rhs) ?: TolkTy.Unknown
            processAssigmentAfterRight(lhs, rightType, nextFlow)
            ctx.setType(element, rightType)
        }
        return TolkExpressionFlowContext(nextFlow, usedAsCondition)
    }

    private fun inferLeftSideVarAssigment(element: TolkVarDefinition, flow: TolkFlowContext): TolkFlowContext {
        var nextFlow = flow
        when (element) {
            is TolkVarTensor -> {
                val tensorElements = element.varDefinitionList
                val tensorElementTypes = ArrayList<TolkTy>(tensorElements.size)
                for (tensorElement in tensorElements) {
                    nextFlow = inferLeftSideVarAssigment(tensorElement, nextFlow)
                    val tensorElementType = ctx.getType(tensorElement) ?: TolkTy.Unknown
                    tensorElementTypes.add(tensorElementType)
                }
                val tensorType = TolkTy.tensor(tensorElementTypes)
                ctx.setType(element, tensorType)
            }

            is TolkVarTuple -> {
                val tupleElements = element.varDefinitionList
                val tupleElementTypes = ArrayList<TolkTy>(tupleElements.size)
                for (tupleElement in tupleElements) {
                    nextFlow = inferLeftSideVarAssigment(tupleElement, nextFlow)
                    val tupleElementType = ctx.getType(tupleElement) ?: TolkTy.Unknown
                    tupleElementTypes.add(tupleElementType)
                }
                val tupleType = TolkTy.typedTuple(tupleElementTypes)
                ctx.setType(element, tupleType)
            }

            is TolkVarRedef -> {
                nextFlow = inferExpression(element.referenceExpression, nextFlow, false).outFlow
                val type = ctx.getType(element.referenceExpression)
                ctx.setType(element, type)
            }

            is TolkVar -> {
                val typeHint = element.typeExpression?.type ?: TolkTy.Unknown
                ctx.setType(element, typeHint)
                nextFlow.setSymbol(element, typeHint)
            }
        }
        return nextFlow
    }

    private fun inferLeftSideAssigment(
        element: TolkExpression,
        flow: TolkFlowContext,
    ): TolkFlowContext {
        var nextFlow = flow
        when (element) {
            is TolkTensorExpression -> {
                val tensorItems = element.expressionList
                val typesList = ArrayList<TolkTy>(tensorItems.size)
                tensorItems.forEach { item ->
                    nextFlow = inferLeftSideAssigment(item, flow)
                    typesList.add(ctx.getType(item) ?: TolkTy.Unknown)
                }
                ctx.setType(element, TolkTy.tensor(typesList))
            }

            is TolkTupleExpression -> {
                val tupleItems = element.expressionList
                val typesList = ArrayList<TolkTy>(tupleItems.size)
                tupleItems.forEach { item ->
                    nextFlow = inferLeftSideAssigment(item, flow)
                    typesList.add(ctx.getType(item) ?: TolkTy.Unknown)
                }
                ctx.setType(element, TolkTy.typedTuple(typesList))
            }

            is TolkParenExpression -> {
                nextFlow = inferLeftSideAssigment(element.expression ?: return nextFlow, flow)
                ctx.setType(element, ctx.getType(element.expression))
            }

            else -> {
                nextFlow = inferExpression(element, nextFlow, false).outFlow
                extractSinkExpression(element)?.let { sExpr ->
                    val lhsDeclaredType = calcDeclaredTypeBeforeSmartCast(element)
                    ctx.setType(element, lhsDeclaredType)
                }
            }
        }
        return nextFlow
    }

    private fun processVarDefinitionAfterRight(
        element: TolkVarDefinition,
        rightType: TolkTy,
        outFlow: TolkFlowContext
    ) {
        when (element) {
            is TolkVar -> {
                val declaredType = element.typeExpression?.type
                val smartCastedType = declaredType?.let {
                    calcSmartcastTypeOnAssignment(declaredType, rightType)
                } ?: rightType
                ctx.setType(element, declaredType ?: rightType)
                outFlow.setSymbol(TolkSinkExpression(element), smartCastedType)
            }

            is TolkVarTensor -> {
                val rhsTensor = rightType.unwrapTypeAlias() as? TolkTensorTy
                val leftElements = element.varDefinitionList
                val typesList = ArrayList<TolkTy>(leftElements.size)
                leftElements.forEachIndexed { index, expression ->
                    val ithRightType = rhsTensor?.elements?.get(index) ?: TolkTy.Unknown
                    processVarDefinitionAfterRight(expression, ithRightType, outFlow)
                    typesList.add(ctx.getType(expression) ?: TolkTy.Unknown)
                }
                val tensorType = TolkTy.tensor(typesList)
                ctx.setType(element, tensorType)
            }

            is TolkVarTuple -> {
                val rhsTuple = rightType.unwrapTypeAlias() as? TolkTypedTupleTy
                val leftElements = element.varDefinitionList
                val typesList = ArrayList<TolkTy>(leftElements.size)
                leftElements.forEachIndexed { index, expression ->
                    val ithRightType = rhsTuple?.elements?.get(index) ?: TolkTy.Unknown
                    processVarDefinitionAfterRight(expression, ithRightType, outFlow)
                    typesList.add(ctx.getType(expression) ?: TolkTy.Unknown)
                }
                val tupleType = TolkTy.typedTuple(typesList)
                ctx.setType(element, tupleType)
            }
        }
    }

    private fun calcSmartcastTypeOnAssignment(
        lhsDeclaredType: TolkTy,
        rhsInferredType: TolkTy,
    ): TolkTy {
        val lhsUnion = lhsDeclaredType.unwrapTypeAlias() as? TolkUnionTy ?: return lhsDeclaredType
        val lhsSubtype = lhsUnion.calculateExactVariantToFitRhs(rhsInferredType)
        if (lhsSubtype != null) {
            return lhsSubtype
        }
        val rhsUnion = rhsInferredType as? TolkUnionTy ?: return lhsDeclaredType

        var lhsHasAllVariantsOfRhs = true
        for (rhsVariant in rhsUnion.variants) {
            lhsHasAllVariantsOfRhs = lhsHasAllVariantsOfRhs and lhsUnion.contains(rhsVariant)
        }

        if (!lhsHasAllVariantsOfRhs || rhsUnion.variants.size >= lhsUnion.variants.size) {
            return lhsDeclaredType
        }

        val subtypesOfLhs = mutableListOf<TolkTy>()
        for (lhsVariant in lhsUnion.variants) {
            if (rhsUnion.contains(lhsVariant)) {
                subtypesOfLhs.add(lhsVariant)
            }
        }
        return TolkUnionTy.create(subtypesOfLhs)
    }

    private fun inferExpression(
        element: TolkExpression,
        flow: TolkFlowContext,
        usedAsCondition: Boolean,
        hint: TolkTy? = null
    ): TolkExpressionFlowContext {
        return when (element) {
            is TolkLiteralExpression -> inferLiteralExpression(element, flow, usedAsCondition)
            is TolkTernaryExpression -> inferTernaryExpression(element, flow, usedAsCondition, hint)
            is TolkBinExpression -> inferBinExpression(element, flow, usedAsCondition)
            is TolkIsExpression -> inferIsExpression(element, flow, usedAsCondition)
            is TolkReferenceExpression -> inferReferenceExpression(element, flow, usedAsCondition).context
            is TolkSelfExpression -> inferSelfExpression(element, flow, usedAsCondition)
            is TolkTupleExpression -> inferTupleExpression(element, flow, usedAsCondition, hint)
            is TolkTensorExpression -> inferTensorExpression(element, flow, usedAsCondition, hint)
            is TolkCallExpression -> inferCallExpression(element, flow, usedAsCondition, hint)
            is TolkDotExpression -> inferDotExpression(element, flow, usedAsCondition, hint)
            is TolkParenExpression -> inferParenExpression(element, flow, usedAsCondition, hint)
            is TolkPrefixExpression -> inferPrefixExpression(element, flow, usedAsCondition, hint)
            is TolkNotNullExpression -> inferNotNullExpression(element, flow, usedAsCondition)
            is TolkAsExpression -> inferAsExpression(element, flow, usedAsCondition)
            is TolkVarExpression -> processVarExpression(element, flow)
            is TolkMatchExpression -> inferMatchExpression(element, flow, usedAsCondition, hint)
            is TolkUnitExpression -> inferUnitExpression(element, flow, usedAsCondition)
            is TolkStructExpression -> inferStructExpression(element, flow, hint ?: TolkTy.Unknown)
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
                TolkTy.TRUE -> true
                TolkTy.FALSE -> false
                is TolkConstantIntTy -> type.value != BigInteger.ZERO
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
        hint: TolkTy?
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
        if (conditionType == TolkTy.TRUE) {
            ctx.setType(
                element,
                ctx.getType(thenBranch)
            )
            return afterTrue
        }

        if (conditionType == TolkTy.FALSE) {
            ctx.setType(
                element,
                ctx.getType(elseBranch)
            )
            return afterFalse
        }

        val thenType = ctx.getType(thenBranch) ?: TolkTy.Unknown
        val elseType = ctx.getType(elseBranch) ?: TolkTy.Unknown
        val resultType = thenType.join(elseType)

        //    TypeInferringUnifyStrategy branches_unifier;
        //    branches_unifier.unify_with(v->get_when_true()->inferred_type, hint);
        //    branches_unifier.unify_with(v->get_when_false()->inferred_type, hint);
        //    if (branches_unifier.is_union_of_different_types()) {
        //      // `... ? intVar : sliceVar` results in `int | slice`, probably it's not what the user expected
        //      // example: `var v = ternary`, show an inference error
        //      // do NOT show an error for `var v: T = ternary` (T is hint); it will be checked by type checker later
        //      if (hint == nullptr || hint == TypeDataUnknown::create()) {
        //        fire(cur_f, v->loc, "types of ternary branches are incompatible: " + to_string(v->get_when_true()) + " and " + to_string(v->get_when_false()));
        //      }
        //    }

        val outFlow = afterTrue.outFlow.join(afterFalse.outFlow)
        ctx.setType(element, resultType)

        return TolkExpressionFlowContext(outFlow, afterTrue.trueFlow, afterFalse.falseFlow)
    }

    private fun inferBinExpression(
        element: TolkBinExpression,
        flow: TolkFlowContext,
        usedAsCondition: Boolean
    ): TolkExpressionFlowContext {
        val binaryOp = element.binaryOp
        val operatorType = binaryOp.node.firstChildNode.elementType

        fun TolkFlowContext.toResult() = TolkExpressionFlowContext(this, usedAsCondition)

        val expressions = element.expressionList
        val left = expressions.first()
        val right = expressions.getOrNull(1)
        var nextFlow = flow
        when (operatorType) {
            TolkElementTypes.EQ -> return inferAssigment(element, flow, usedAsCondition)
            TolkElementTypes.ANDAND -> {
                ctx.setType(element, TolkTy.Bool)
                val afterLeft = inferExpression(left, nextFlow, true)
                if (right == null) {
                    return afterLeft.outFlow.toResult()
                }
                val afterRight = inferExpression(right, afterLeft.trueFlow, true)
                if (!usedAsCondition) {
                    val outFlow = afterLeft.falseFlow.join(afterRight.outFlow)
                    val leftType = ctx.getType(left) ?: TolkTy.Unknown
                    val rightType = ctx.getType(right) ?: TolkTy.Unknown
                    val elementType = leftType.join(rightType)
                    ctx.setType(element, elementType)
                    return TolkExpressionFlowContext(outFlow, false)
                }
                val outFlow = afterLeft.outFlow.join(afterRight.outFlow)
                val leftType = ctx.getType(left) ?: TolkTy.Unknown
                val rightType = ctx.getType(right) ?: TolkTy.Unknown
                val trueFlow = afterRight.trueFlow
                val falseFlow = afterLeft.falseFlow.join(afterRight.falseFlow)
                val elementType = leftType.join(rightType)
                ctx.setType(element, elementType)
                return TolkExpressionFlowContext(outFlow, trueFlow, falseFlow)
            }

            TolkElementTypes.OROR -> {
                ctx.setType(element, TolkTy.Bool)
                val afterLeft = inferExpression(left, nextFlow, true)
                if (right == null) {
                    return afterLeft.outFlow.toResult()
                }
                val afterRight = inferExpression(right, afterLeft.falseFlow, true)
                if (!usedAsCondition) {
                    val outFlow = afterLeft.trueFlow.join(afterRight.outFlow)
                    val leftType = ctx.getType(left) ?: TolkTy.Unknown
                    val rightType = ctx.getType(right) ?: TolkTy.Unknown
                    val elementType = leftType.join(rightType)
                    ctx.setType(element, elementType)
                    return TolkExpressionFlowContext(outFlow, false)
                }
                val outFlow = afterLeft.outFlow.join(afterRight.outFlow)
                val trueFlow = afterLeft.trueFlow.join(afterRight.trueFlow)
                val falseFlow = afterRight.falseFlow
                val leftType = ctx.getType(left) ?: TolkTy.Unknown
                val rightType = ctx.getType(right) ?: TolkTy.Unknown
                val elementType = leftType.join(rightType)
                ctx.setType(element, elementType)

                return TolkExpressionFlowContext(outFlow, trueFlow, falseFlow)
            }

            in ASIGMENT_OPERATORS -> return inferSetAssigment(element, flow, usedAsCondition)

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
                ctx.setType(element, TolkTy.Bool)
            }

            in AND_OR_XOR_OPERATORS -> {
                nextFlow = inferExpression(left, nextFlow, true).outFlow
                if (right != null) {
                    nextFlow = inferExpression(right, nextFlow, true).outFlow
                }
                val leftType = ctx.getType(left) ?: TolkTy.Unknown
                val rightType = right?.let { ctx.getType(right) } ?: TolkTy.Unknown

                val elementType = if (leftType is TolkConstantBoolTy && rightType is TolkConstantBoolTy) {
                    when (operatorType) {
                        TolkElementTypes.AND -> TolkTy.bool(leftType.value.and(rightType.value))
                        TolkElementTypes.OR -> TolkTy.bool(leftType.value.or(rightType.value))
                        TolkElementTypes.XOR -> TolkTy.bool(leftType.value.xor(rightType.value))
                        else -> leftType.join(rightType)
                    }
                } else {
                    leftType.join(rightType)
                }

                ctx.setType(element, elementType)
            }

            else -> {
                nextFlow = inferExpression(left, nextFlow, false).outFlow
                if (right == null) {
                    ctx.setType(element, TolkTy.Int)
                    return nextFlow.toResult()
                }
                nextFlow = inferExpression(right, nextFlow, false).outFlow
                val leftType = ctx.getType(left)
                val rightType = ctx.getType(right)

                if (leftType != null && rightType != null) {
                    ctx.setType(
                        element, try {
                            leftType.join(rightType)
                        } catch (e: IllegalStateException) {
//                        IllegalStateException("Can't join $leftType and $rightType in $currentFunction", e).printStackTrace()
                            TolkTy.Int
                        }
                    )
                } else {
                    ctx.setType(element, leftType ?: rightType)
                }
            }
        }

        return nextFlow.toResult()
    }

    private fun inferIsExpression(
        element: TolkIsExpression,
        flow: TolkFlowContext,
        usedAsCondition: Boolean
    ): TolkExpressionFlowContext {
        val expression = element.expression
        val afterExpr = inferExpression(expression, flow, false)

        val isExprType = element.typeExpression?.type
        var rhsType = isExprType?.unwrapTypeAlias()
        val exprType = ctx.getType(expression)?.unwrapTypeAlias() ?: TolkTy.Unknown
        if (rhsType is TolkStructTy) {
            tryPickInstantiatedGenericFromHint(exprType, rhsType.psi)?.let {
                rhsType = it
            }
        }
        val nonRhsType = exprType.subtract(rhsType)
        val isNegated = element.node.findChildByType(TolkElementTypes.NOT_IS_KEYWORD) != null
        var resultType: TolkBoolTy = TolkTy.Bool
        if (exprType == rhsType) {
            // `expr is <type>` is always true
            resultType = if (isNegated) TolkTy.FALSE else TolkTy.TRUE
        } else if (nonRhsType == TolkTy.Never) {
            // `expr is <type>` is always false
            resultType = if (isNegated) TolkTy.TRUE else TolkTy.FALSE
        }

        ctx.setType(element, resultType)

        if (!usedAsCondition) {
            return afterExpr
        }

        val trueFlow = afterExpr.outFlow.clone()
        val falseFlow = afterExpr.outFlow.clone()
        val sExpr = extractSinkExpression(expression)
        if (sExpr != null) {
            if (resultType == TolkTy.TRUE) {
                falseFlow.unreachable = TolkUnreachableKind.CantHappen
                falseFlow.setSymbol(sExpr, TolkTy.Never)
            } else if (resultType == TolkTy.FALSE) {
                trueFlow.unreachable = TolkUnreachableKind.CantHappen
                trueFlow.setSymbol(sExpr, TolkTy.Never)
            } else if (!isNegated) {
                trueFlow.setSymbol(sExpr, rhsType ?: TolkTy.Unknown)
                falseFlow.setSymbol(sExpr, nonRhsType)
            } else {
                trueFlow.setSymbol(sExpr, nonRhsType)
                falseFlow.setSymbol(sExpr, rhsType ?: TolkTy.Unknown)
            }
        }

        return TolkExpressionFlowContext(afterExpr.outFlow, trueFlow, falseFlow)
    }

    private fun inferIsNullCheck(
        element: TolkBinExpression,
        expression: TolkExpression,
        isInverted: Boolean,
        flow: TolkFlowContext,
        usedAsCondition: Boolean
    ): TolkExpressionFlowContext {
        val afterExpr = inferExpression(expression, flow, false)

        val exprType = ctx.getType(expression)?.unwrapTypeAlias() ?: return afterExpr
        val notNullType = exprType.subtract(TolkTy.Null)
        val resultType = if (exprType == TolkTy.Null) { // `expr == null` is always true
            if (isInverted) TolkTy.FALSE else TolkTy.TRUE
        } else if (notNullType == TolkTy.Never) { // `expr == null` is always false
            if (isInverted) TolkTy.TRUE else TolkTy.FALSE
        } else {
            TolkTy.Bool
        }
        ctx.setType(element, resultType)

        if (!usedAsCondition) {
            return afterExpr
        }

        val trueFlow = TolkFlowContext(afterExpr.outFlow)
        val falseFlow = TolkFlowContext(afterExpr.outFlow)
        val sExpr = extractSinkExpression(expression) ?: return afterExpr
        if (resultType == TolkTy.TRUE) {
            falseFlow.unreachable = TolkUnreachableKind.CantHappen
            falseFlow.setSymbol(sExpr, TolkTy.Never)
        } else if (resultType == TolkTy.FALSE) {
            trueFlow.unreachable = TolkUnreachableKind.CantHappen
            trueFlow.setSymbol(sExpr, TolkTy.Never)
        } else if (!isInverted) {
            trueFlow.setSymbol(sExpr, TolkTy.Null)
            falseFlow.setSymbol(sExpr, notNullType)
        } else {
            trueFlow.setSymbol(sExpr, notNullType)
            falseFlow.setSymbol(sExpr, TolkTy.Null)
        }

        return TolkExpressionFlowContext(afterExpr.outFlow, trueFlow, falseFlow)
    }

    private fun inferSetAssigment(
        element: TolkBinExpression,
        flow: TolkFlowContext,
        usedAsCondition: Boolean
    ): TolkExpressionFlowContext {
        val lhs = element.left
        val afterLhs = inferExpression(lhs, flow, false)
        val rhs = element.right ?: return afterLhs
        val rhsFlow = afterLhs.outFlow
        val afterRhs = inferExpression(rhs, rhsFlow, false, ctx.getType(lhs))

//        val builtinFuncName = "_${element.binaryOp.text}_"
//        val builtinFunc = TolkBuiltins[project].getFunction(builtinFuncName)
//        builtinFunc?.type
        val resultType = ctx.getType(rhs)?.actualType() ?: TolkTy.Unknown
        ctx.setType(element, resultType)

        return TolkExpressionFlowContext(afterRhs.outFlow, usedAsCondition)
    }

    private fun processAssigmentAfterRight(
        left: TolkExpression,
        rightType: TolkTy,
        flow: TolkFlowContext
    ) {
        when (left) {
            is TolkReferenceExpression -> {
                val resolvedElement = extractSinkExpression(left)
                if (resolvedElement != null) {
                    flow.setSymbol(resolvedElement, rightType)
                }
            }

            is TolkTensorExpression -> {
                val rhsTensor = rightType as? TolkTensorTy
                val tensorItems = left.expressionList
                val typesList = ArrayList<TolkTy>(tensorItems.size)
                tensorItems.forEachIndexed { index, item ->
                    val ithRhsType = rhsTensor?.elements?.getOrNull(index) ?: TolkTy.Unknown
                    processAssigmentAfterRight(item, ithRhsType, flow)
                    typesList.add(ctx.getType(item) ?: TolkTy.Unknown)
                }
                ctx.setType(left, TolkTy.tensor(typesList))
            }

            is TolkTupleExpression -> {
                val rhsTuple = rightType as? TolkTypedTupleTy
                val tupleItems = left.expressionList
                val typesList = ArrayList<TolkTy>(tupleItems.size)
                tupleItems.forEachIndexed { index, item ->
                    val ithRhsType = rhsTuple?.elements?.getOrNull(index) ?: TolkTy.Unknown
                    processAssigmentAfterRight(item, ithRhsType, flow)
                    typesList.add(ctx.getType(item) ?: TolkTy.Unknown)
                }
                ctx.setType(left, TolkTy.typedTuple(typesList))
            }

            is TolkParenExpression -> {
                processAssigmentAfterRight(left.expression ?: return, rightType, flow)
                ctx.setType(left, ctx.getType(left.expression))
            }

            else -> {
                val sExpr = extractSinkExpression(left) ?: return
                val lhsDeclaredType = ctx.getType(left) ?: return
                val smartCastedType = calcSmartcastTypeOnAssignment(lhsDeclaredType, rightType)
                flow.setSymbol(sExpr, smartCastedType)
            }
        }
    }

    private fun inferReferenceExpression(
        element: TolkReferenceExpression,
        flow: TolkFlowContext,
        usedAsCondition: Boolean,
    ): TolkExpressionInferenceResult {
        val nextFlow = TolkExpressionFlowContext(flow, usedAsCondition)
        val name = element.referenceName ?: return TolkExpressionInferenceResult(
            element,
            nextFlow
        )

        val variableCandidate = localSymbols[element]
        if (variableCandidate != null) {
            val variableType = flow.getType(TolkSinkExpression(variableCandidate)) ?: try {
                variableCandidate.type
            } catch (_: CyclicReferenceException) {
                // cyclic resolve for variables = `var a = a` // TODO; fix resolving to self?
                return TolkExpressionInferenceResult(element, nextFlow)
            }
            ctx.setType(element, variableType)
            return TolkExpressionInferenceResult(element, nextFlow, variableType)
        }

        val genericTy = currentFunction?.resolveGenericType(name)
        if (genericTy != null) {
            ctx.setType(element, genericTy)
            ctx.setResolvedRefs(element, listOf(PsiElementResolveResult(genericTy.parameter.psi)))
            return TolkExpressionInferenceResult(
                element,
                nextFlow,
                genericTy
            )
        }

        val symbolCandidates = resolveToGlobalSymbols(element, name)
        if (symbolCandidates.isNotEmpty()) {
            ctx.setResolvedRefs(element, symbolCandidates.map { PsiElementResolveResult(it) })
            var type = try {
                symbolCandidates.first().type
            } catch (_: CyclicReferenceException) {
                // cyclic resolve for constants = `const a = b; const b = a`
                return TolkExpressionInferenceResult(element, nextFlow)
            }
            if (type is TolkStructTy && type.hasGenerics()) {
                val typeArguments = element.typeArgumentList?.typeExpressionList
                val substitution = HashMap<TolkTypeParameterTy, TolkTy>()
                type.typeArguments.forEachIndexed { index, typeParam ->
                    if (typeParam !is TolkTypeParameterTy) return@forEachIndexed
                    val parameter =
                        typeParam.parameter as? TolkTypeParameterTy.NamedTypeParameter ?: return@forEachIndexed
                    val subType = typeArguments?.getOrNull(index)?.type
                        ?: parameter.psi.defaultTypeParameter?.typeExpression?.type
                    if (subType != null) {
                        substitution[typeParam] = subType
                    }
                }

                type = type.substitute(Substitution(substitution))
            }

            ctx.setType(element, type)
            return TolkExpressionInferenceResult(
                element,
                nextFlow,
                type
            )
        }

        val functionCandidates = resolveToFunction(element, name)
        if (functionCandidates.isNotEmpty()) {
            ctx.setResolvedRefs(element, functionCandidates.map { PsiElementResolveResult(it.first) })

            val singleCandidate = functionCandidates.singleOrNull()
            if (singleCandidate != null) {
                val (function, sub) = singleCandidate
                val typeArgumentList = element.typeArgumentList?.typeExpressionList ?: emptyList()
                val substituteMap = HashMap<TolkTypeParameterTy, TolkTy>()
                val typeParameterList = function.typeParameterList?.typeParameterList ?: emptyList()
                typeParameterList.zip(typeArgumentList).forEach { (typeParameter, typeArgument) ->
                    substituteMap[TolkTypeParameterTy.create(typeParameter)] = typeArgument.type ?: TolkTy.Unknown
                }
                val functionDeclaredType = function.declaredType
                if (functionDeclaredType.hasGenerics()) {
                    val substitution = sub + Substitution(substituteMap)
                    val subType = functionDeclaredType.substitute(substitution)
                    ctx.setType(element, subType)
                    return TolkExpressionInferenceResult(
                        element,
                        nextFlow,
                        subType,
                        substitution
                    )
                } else {
                    ctx.setType(element, functionDeclaredType)
                    return TolkExpressionInferenceResult(
                        element,
                        nextFlow,
                        functionDeclaredType,
                        sub
                    )
                }
            }
        }

        return TolkExpressionInferenceResult(
            element,
            nextFlow
        )
    }

    private fun resolveToVariable(
        element: TolkReferenceExpression,
        flow: TolkFlowContext,
    ): TolkSymbolElement? {
        val name = element.referenceName ?: return null
        return flow.getSymbol(name)
    }

    private fun resolveToFunction(
        element: TolkReferenceElement,
        name: String
    ): List<Pair<TolkFunction, Substitution>> {
        val builtinFunction = TolkBuiltins[project].getFunction(name)
        return if (builtinFunction != null) {
            SmartList(builtinFunction to EmptySubstitution)
        } else {
            collectFunctionCandidates(
                project,
                null,
                name,
                element.containingFile as TolkFile,
            )
        }
    }

    private fun resolveToGlobalSymbols(
        element: TolkReferenceElement,
        name: String
    ): List<TolkSymbolElement> {
        val declarations = (element.containingFile as TolkFile).resolveSymbols(name)
        val result = SmartList<TolkSymbolElement>()
        declarations.forEach {
            when (it) {
                is TolkTypeSymbolElement,
                is TolkGlobalVar,
                is TolkConstVar -> {
                    result.add(it)
                }
            }
        }
        return result
    }

    private fun inferSelfExpression(
        element: TolkSelfExpression,
        flow: TolkFlowContext,
        usedAsCondition: Boolean,
    ): TolkExpressionFlowContext {
        val selfParam = element.reference?.resolve() as? TolkSelfParameter
        if (selfParam != null) {
            val type = flow.getType(TolkSinkExpression(selfParam)) ?: flow.getType(selfParam)
            ctx.setType(element, type)
        }
        return TolkExpressionFlowContext(flow, usedAsCondition)
    }

    private fun inferTupleExpression(
        element: TolkTupleExpression,
        flow: TolkFlowContext,
        usedAsCondition: Boolean,
        hint: TolkTy?
    ): TolkExpressionFlowContext {
        val tupleHint = hint as? TolkTypedTupleTy
        val typesList = ArrayList<TolkTy>(element.expressionList.size)
        var nextFlow = flow
        for (item in element.expressionList) {
            nextFlow =
                inferExpression(item, nextFlow, false, tupleHint?.elements?.getOrNull(typesList.size)).outFlow
            val type = ctx.getType(item) ?: TolkTy.Unknown
            typesList.add(type)
        }
        val tupleType = TolkTy.typedTuple(typesList)
        ctx.setType(element, tupleType)
        return TolkExpressionFlowContext(nextFlow, usedAsCondition)
    }

    private fun inferTensorExpression(
        element: TolkTensorExpression,
        flow: TolkFlowContext,
        usedAsCondition: Boolean,
        hint: TolkTy?
    ): TolkExpressionFlowContext {
        val tensorHint = hint as? TolkTensorTy
        val typesList = ArrayList<TolkTy>(element.expressionList.size)
        var nextFlow = flow
        element.expressionList.forEachIndexed { index, item ->
            val tensorItemType = tensorHint?.elements?.getOrNull(typesList.size)
            nextFlow = inferExpression(item, nextFlow, false, tensorItemType).outFlow
            val itemType = ctx.getType(item) ?: TolkTy.Unknown
            typesList.add(itemType)
        }
        val tensorType = TolkTy.tensor(typesList)
        ctx.setType(element, tensorType)
        return TolkExpressionFlowContext(nextFlow, usedAsCondition)
    }

    private fun inferCallExpression(
        element: TolkCallExpression,
        flow: TolkFlowContext,
        usedAsCondition: Boolean,
        hint: TolkTy?
    ): TolkExpressionFlowContext {
        var nextFlow = flow
        val callee = element.expression
        var functionSymbol: TolkFunction? = null
        var sub: Substitution = EmptySubstitution
        var functionType: TolkFunctionTy? = null

        when (callee) {
            is TolkReferenceExpression -> {
                val refInfResult = inferReferenceExpression(callee, nextFlow, false)
                nextFlow = refInfResult.context.outFlow
                functionSymbol = ctx.getResolvedRefs(callee).firstOrNull()?.element as? TolkFunction
                sub = refInfResult.substitution
                functionType = (refInfResult.inferredType as? TolkFunctionTy)?.let {
                    sub = sub.deduce(functionSymbol?.type as? TolkFunctionTy ?: it, it)
                    it
                }
            }

            is TolkDotExpression -> {
                nextFlow = inferDotExpression(callee, nextFlow, false).outFlow
                val calleeRight = callee.fieldLookup
                if (calleeRight != null) {
                    functionSymbol = ctx.getResolvedFields(calleeRight).firstOrNull() as? TolkFunction
                    functionType = (ctx.getType(callee) as? TolkFunctionTy)?.let {
                        sub = sub.deduce(functionSymbol?.type as? TolkFunctionTy ?: it, it)
                        it
                    }
                }
            }

            else -> {
                nextFlow = inferExpression(callee, nextFlow, false).outFlow
                functionSymbol = ctx.getResolvedRefs(callee).firstOrNull()?.element as? TolkFunction
            }
        }

        // handle `local_var()` / `getF()()` / `5()` / `SOME_CONST()` / `obj.method()()()` / `tensorVar.0()`
        if (functionSymbol == null) {
            val callableFunction = ctx.getType(callee)
            if (callableFunction !is TolkFunctionTy) {
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
        ctx.setResolvedFunctions(element, functionSymbol)

        // so, we have a call `f(args)` or `obj.f(args)`, f is fun_ref (function / method) (code / asm / builtin)
        // we're going to iterate over passed arguments, and (if generic) infer substitutedTs
        // at first, check arguments count (Tolk doesn't have optional parameters, so just compare counts)

        iterateOverParameters(
            element,
            referenceResolver = {
                ctx.getResolvedRefs(it).firstOrNull()?.element
            }
        ) { param, arg ->
            if (arg == null) {
                return@iterateOverParameters
            }
            var paramType = param.type ?: TolkTy.Unknown
            if (paramType.hasGenerics()) {
                paramType = paramType.substitute(sub)
            }
            val argExpr = arg.expression
            nextFlow = inferExpression(argExpr, nextFlow, false, paramType).outFlow
            var argType = ctx.getType(argExpr) ?: TolkTy.Unknown
            sub = sub.deduce(paramType, argType)
            argType = argType.substitute(sub)
            ctx.setType(argExpr, argType)
            if (param.isMutable && argType.unwrapTypeAlias() != paramType.unwrapTypeAlias()) {
                val sExpr = extractSinkExpression(argExpr)
                if (sExpr != null) {
                    ctx.setType(argExpr, calcDeclaredTypeBeforeSmartCast(argExpr))
                    nextFlow.setSymbol(sExpr, paramType)
                }
            }
        }

        val functionDeclaredType = functionType ?: functionSymbol.declaredType
        val functionSubType = if (functionDeclaredType.hasGenerics()) {
            functionSymbol.declaredType.substitute(sub) as TolkFunctionTy
        } else {
            functionDeclaredType
        }
        val returnType = functionSubType.returnType

        ctx.setType(callee, functionSubType)
        ctx.setType(element, returnType)

        return TolkExpressionFlowContext(nextFlow, usedAsCondition)
    }

    private fun inferDotExpression(
        element: TolkDotExpression,
        flow: TolkFlowContext,
        usedAsCondition: Boolean,
        hint: TolkTy? = null
    ): TolkExpressionFlowContext {
        var nextFlow = flow
        val receiver = element.expression
        nextFlow = inferExpression(receiver, nextFlow, false).outFlow
        val receiverSymbol = ctx.getResolvedRefs(receiver).singleOrNull()?.element
        val receiverType =
            if (receiver is TolkReferenceExpression && (receiverSymbol == null || receiverSymbol is TolkFunction)) {
                val name = receiver.referenceName
                if (name != null) {
                    TolkTy.byName(name)
                } else {
                    null
                }
            } else {
                ctx.getType(receiver)?.actualType()
            }

        val fieldLookup = element.fieldLookup ?: return TolkExpressionFlowContext(nextFlow, usedAsCondition)
        var inferredType = inferFieldLookup(receiverType ?: TolkTy.Unknown, fieldLookup, hint)
        extractSinkExpression(element)?.let { sExpr ->
            nextFlow.getType(sExpr)?.let { smartCasted ->
                inferredType = smartCasted
            }
        }

        ctx.setType(element, inferredType)
        return TolkExpressionFlowContext(nextFlow, usedAsCondition)
    }

    private fun inferFieldLookup(
        receiver: TolkTy,
        fieldLookup: TolkFieldLookup,
        hint: TolkTy?
    ): TolkTy {
        val variants = resolveFieldLookupReferenceWithReceiver(receiver, fieldLookup)
        val firstVariant = variants.firstOrNull()
        if (firstVariant == null) {
            val receiverType = receiver.unwrapTypeAlias().actualType()
            val types = when (receiverType) {
                is TolkTypedTupleTy -> receiverType.elements
                is TolkTensorTy -> receiverType.elements
                else -> return TolkTy.Unknown
            }
            val fieldIndex = fieldLookup.integerLiteral?.text?.toIntOrNull() ?: return TolkTy.Unknown
            return types.getOrNull(fieldIndex) ?: TolkTy.Unknown
        } else {
            ctx.setResolvedRefs(fieldLookup, variants.map { PsiElementResolveResult(it.first ) })
            val (resolved, sub) = firstVariant
            val rawFieldType = resolved.type ?: hint ?: TolkTy.Unknown
            return rawFieldType.substitute(sub)
        }
    }

    private fun inferParenExpression(
        element: TolkParenExpression,
        flow: TolkFlowContext,
        usedAsCondition: Boolean,
        hint: TolkTy?
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
        hint: TolkTy?
    ): TolkExpressionFlowContext {
        val expression = element.expression ?: return TolkExpressionFlowContext(flow, usedAsCondition)
        var afterExpr = inferExpression(expression, flow, usedAsCondition, hint)
        val operatorType = element.node.firstChildNode.elementType
        when (operatorType) {
            TolkElementTypes.MINUS -> {
                val expressionType = ctx.getType(expression) as? TolkIntTy ?: TolkTy.Int
                val resultType = expressionType.negate()
                ctx.setType(element, resultType)
            }

            TolkElementTypes.EXCL -> {
                val expressionType = ctx.getType(expression) as? TolkBoolTy ?: TolkTy.Bool
                val resultType = expressionType.negate()
                ctx.setType(element, resultType)
                afterExpr = TolkExpressionFlowContext(afterExpr.outFlow, afterExpr.falseFlow, afterExpr.trueFlow)
            }

            in INT_PREFIX_OPERATORS -> {
                ctx.setType(element, TolkTy.Int)
            }

            else -> {
                val expressionType = ctx.getType(expression)
                ctx.setType(element, expressionType)
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
        val exprType = ctx.getType(expression)
        val withoutNull = exprType?.unwrapTypeAlias()?.subtract(TolkTy.Null)
        val actualType = if (withoutNull != TolkTy.Never) withoutNull else exprType
        ctx.setType(element, actualType)
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
        val expression = try {
            element.expression
        } catch (e: Exception) {
            throw e
        }
        val asType = element.typeExpression?.type
        val afterExpr = inferExpression(expression, flow, false, asType)
        ctx.setType(element, asType)
        if (!usedAsCondition) {
            return afterExpr
        }

        return TolkExpressionFlowContext(afterExpr.outFlow, true)
    }

    private fun inferMatchExpression(
        element: TolkMatchExpression,
        flow: TolkFlowContext,
        usedAsCondition: Boolean,
        hint: TolkTy?
    ): TolkExpressionFlowContext {
        val expression = element.expression ?: return TolkExpressionFlowContext(flow, usedAsCondition)
        val afterExpr = inferExpression(expression, flow, false).outFlow
        val sinkExpression = extractSinkExpression(expression)

        val armsEntryFlow = afterExpr.clone()

        var matchOutFlow: TolkFlowContext? = null
        var unifiedType: TolkTy? = null
        element.matchArmList.forEach { matchArm ->
            val matchBody = matchArm.matchBody
            val matchExpression = matchBody?.expression
            val matchPattern = matchArm.matchPattern

            var armFlow: TolkFlowContext? = null
            val matchPatternExpression = matchPattern.expression
            if (matchPatternExpression != null) {
                armFlow = inferExpression(matchPatternExpression, armsEntryFlow.clone(), usedAsCondition).outFlow
            }
            if (armFlow == null) {
                armFlow = armsEntryFlow.clone()
            }
            val matchPatternReference = matchPattern.matchPatternReference
            if (matchPatternReference != null) {
                val name = matchPatternReference.identifier.text.removeSurrounding("`")
                val symbol = armsEntryFlow.getSymbol(name)

                val syncExprType = if (symbol != null) {
                    ctx.setResolvedRefs(matchPatternReference, listOf(PsiElementResolveResult(symbol)))
                    ctx.getType(symbol) ?: symbol.type
                } else {
                    var exactType: TolkTy? = TolkPrimitiveTy.fromName(name)
                    if (exactType == null) {
                        val result = TolkTypeReference(matchPatternReference)
                            .multiResolve(false).firstOrNull()?.element as? TolkTypedElement
                        exactType = result?.type
                    }
                    exactType
                }
                if (sinkExpression != null && syncExprType != null) {
                    armFlow.setSymbol(sinkExpression, syncExprType)
                }
            }
            if (sinkExpression != null) {
                val exactType = matchArm.matchPattern.typeExpression?.type
                if (exactType != null) {
                    armFlow.setSymbol(sinkExpression, exactType)
                }
            }
            if (matchExpression != null) {
                armFlow = inferExpression(matchExpression, armFlow, usedAsCondition).outFlow
                matchOutFlow = matchOutFlow.join(armFlow)
                val exprType = ctx.getType(matchExpression)
                unifiedType = exprType.join(unifiedType ?: hint)
                return@forEach
            }
            val returnStatement = matchBody?.returnStatement
            if (returnStatement != null) {
                armFlow = inferStatement(returnStatement, armFlow)
                matchOutFlow = matchOutFlow.join(armFlow)
                return@forEach
            }
            val throwStatement = matchBody?.throwStatement
            if (throwStatement != null) {
                armFlow = inferStatement(throwStatement, armFlow)
                matchOutFlow = matchOutFlow.join(armFlow)
                return@forEach
            }
            val blockStatement = matchBody?.blockStatement
            if (blockStatement != null) {
                val armFlow = inferStatement(blockStatement, armFlow)
                matchOutFlow = matchOutFlow.join(armFlow)
                return@forEach
            }
        }
        ctx.setType(element, unifiedType)

        return TolkExpressionFlowContext(matchOutFlow ?: afterExpr, usedAsCondition)
    }

    private fun inferUnitExpression(
        element: TolkUnitExpression,
        flow: TolkFlowContext,
        usedAsCondition: Boolean
    ): TolkExpressionFlowContext {
        ctx.setType(element, TolkTy.Void)
        return TolkExpressionFlowContext(flow, usedAsCondition)
    }

    private fun inferStructExpression(
        element: TolkStructExpression,
        flow: TolkFlowContext,
        hint: TolkTy
    ): TolkExpressionFlowContext {
        val nextFlow = TolkExpressionFlowContext(flow, false)
        var structType = element.referenceTypeExpression?.type?.unwrapTypeAlias() as? TolkStructTy
        if (structType != null && structType.hasGenerics() && hint != TolkTy.Unknown) {
            val instantiate = Substitution.instantiate(structType, hint.unwrapTypeAlias())
            structType = structType.substitute(instantiate) as? TolkStructTy ?: structType
        }
        if (structType == null && hint != TolkTy.Unknown) {
            val unwrappedHint = hint.unwrapTypeAlias()
            when (unwrappedHint) {
                is TolkStructTy -> structType = unwrappedHint
                is TolkUnionTy -> {
                    var found = 0
                    var lastStruct: TolkStructTy? = null
                    for (hintVariant in unwrappedHint.variants) {
                        val unwrappedHint = hintVariant.unwrapTypeAlias()
                        if (unwrappedHint is TolkStructTy) {
                            lastStruct = unwrappedHint
                            found++
                        }
                    }
                    if (found == 1) {
                        structType = lastStruct
                    }
                }
            }
        }

        var substitution: Substitution = EmptySubstitution
        val body = element.structExpressionBody
        val structPsi = structType?.psi
        if (structPsi != null) {
            substitution = substitution.deduce(structPsi.declaredType, structType)
        }
        body.structExpressionFieldList.forEach { field ->
            val expression = field.expression
            val name = field.identifier.text.removeSurrounding("`")
            if (expression != null) { // field: expression
                val field = structPsi.structFields.firstOrNull { it.name == name }
                var fieldType = field?.type
                if (fieldType != null && fieldType.hasGenerics()) {
                    fieldType = fieldType.substitute(substitution)
                }
                inferExpression(expression, nextFlow.outFlow, false, fieldType)
                val expressionType = ctx.getType(expression)
                if (fieldType != null && expressionType != null) {
                    substitution = substitution.deduce(fieldType, expressionType.actualType())
                    val subType = expressionType.substitute(substitution)
                    ctx.setType(expression, subType)
                }
            } else { // let foo = 1; MyStruct { foo };
                val localSymbol = flow.getSymbol(name)
                if (localSymbol != null) {
                    ctx.setResolvedRefs(field, listOf(PsiElementResolveResult(localSymbol)))
                }
            }
        }

        val type = structType ?: return nextFlow
        val subType = if (type.hasGenerics()) {
            type.substitute(substitution)
        } else {
            type
        }
        ctx.setType(element, subType)

        return nextFlow
    }

    private fun extractSinkExpression(expression: TolkExpression): TolkSinkExpression? {
        return when (expression) {
            is TolkSelfExpression -> {
                val selfParam = expression.reference?.resolve() as? TolkSelfParameter ?: return null
                return TolkSinkExpression(selfParam)
            }

            is TolkReferenceExpression -> {
                val symbol =
                    ctx.getResolvedRefs(expression).firstOrNull()?.element as? TolkSymbolElement ?: return null
                return TolkSinkExpression(symbol)
            }

            is TolkDotExpression -> {
                var currentDot: TolkDotExpression = expression
                var indexPath = 0L
                while (true) {
                    var indexAt = currentDot.targetIndex
                    if (indexAt == null) {
                        val fieldLookup = expression.fieldLookup ?: break
                        val structField =
                            ctx.getResolvedFields(fieldLookup).firstOrNull() as? TolkStructField ?: break
                        indexAt = structField.parent.children.indexOf(structField)
                    }
                    if (indexAt !in 0..255) break
                    indexPath = (indexPath shl 8) + indexAt + 1
                    currentDot = currentDot.expression.unwrapNotNull() as? TolkDotExpression ?: break
                }
                if (indexPath == 0L) return null
                val ref = currentDot.expression.unwrapNotNull() as? TolkReferenceExpression ?: return null
                val symbol =
                    ctx.getResolvedRefs(ref).firstOrNull()?.element as? TolkLocalSymbolElement ?: return null
                return TolkSinkExpression(symbol, indexPath)
            }

            is TolkParenExpression -> extractSinkExpression(expression.unwrapParen())
            is TolkBinExpression -> extractSinkExpression(expression.left)
            is TolkVarExpression -> {
                val varDef = expression.varDefinition as? TolkVar ?: return null
                TolkSinkExpression(varDef)
            }

            else -> null
        }
    }

    private fun calcDeclaredTypeBeforeSmartCast(expression: TolkExpression): TolkTy? {
        when (expression) {
            is TolkReferenceExpression -> {
                val symbol = ctx.getResolvedRefs(expression).firstOrNull()?.element as? TolkSymbolElement
                if (symbol is TolkVar) {
                    return ctx.getType(symbol)
                }
                return symbol?.type
            }

            is TolkDotExpression -> {
                val leftType = calcDeclaredTypeBeforeSmartCast(expression.expression) ?: return null
                return when (leftType) {
                    is TolkStructTy -> {
                        val right = expression.fieldLookup ?: return null
                        val field =
                            ctx.getResolvedFields(right).firstOrNull() as? TolkStructField ?: return null
                        field.type
                    }

                    is TolkTensorTy -> {
                        val index = expression.targetIndex ?: return null
                        leftType.elements.getOrNull(index)
                    }

                    is TolkTypedTupleTy -> {
                        val index = expression.targetIndex ?: return null
                        leftType.elements.getOrNull(index)
                    }

                    else -> null
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

    // helper function: given hint = `Ok<int> | Err<slice>` and struct `Ok`, return `Ok<int>`
    // example: `match (...) { Ok => ... }` we need to deduce `Ok<T>` based on subject
    private fun tryPickInstantiatedGenericFromHint(hint: TolkTy, lookupRef: TolkStruct): TolkStructTy? {
        // example: `var w: Ok<int> = Ok { ... }`, hint is `Ok<int>`, lookup is `Ok`
        (hint.unwrapTypeAlias() as? TolkStructTy)?.let { hStruct ->
            if (lookupRef.isEquivalentTo(hStruct.psi)) {
                return hStruct
            }
        }
        // example: `fun f(): Response<int, slice> { return Err { ... } }`, hint is `Ok<int> | Err<slice>`, lookup is `Err`
        (hint.unwrapTypeAlias() as? TolkUnionTy)?.let { hUnion ->
            var onlyVariant: TolkStructTy? = null
            for (variant in hUnion.variants) {
                val unwrappedVariant = variant.unwrapTypeAlias()
                if (unwrappedVariant is TolkStructTy && lookupRef.isEquivalentTo(unwrappedVariant.psi)) {
                    if (onlyVariant != null) {
                        return null
                    }
                    onlyVariant = unwrappedVariant
                }
            }
            return onlyVariant
        }
        return null
    }
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
