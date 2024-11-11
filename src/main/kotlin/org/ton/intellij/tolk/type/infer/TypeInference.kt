package org.ton.intellij.tolk.type.infer

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.findPsiFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.util.containers.OrderedSet
import org.ton.intellij.tolk.diagnostics.TolkDiagnostic
import org.ton.intellij.tolk.psi.*
import org.ton.intellij.tolk.psi.impl.TolkIncludeDefinitionMixin
import org.ton.intellij.tolk.psi.impl.resolveFile
import org.ton.intellij.tolk.type.ty.TolkTy
import org.ton.intellij.tolk.type.ty.TolkTyUnknown
import org.ton.intellij.util.recursionGuard

fun inferTypesIn(element: TolkInferenceContextOwner): TolkInferenceResult {
    val ctx = TolkInferenceContext(element.project)
    return recursionGuard(element, memoize = false) { ctx.infer(element) }
        ?: error("Can not run nested type inference")
}

interface TolkInferenceData {
    fun getExprTy(expr: TolkExpression): TolkTy

    fun getResolvedRefs(element: TolkReferenceExpression): OrderedSet<PsiElementResolveResult>
}

private val EMPTY_RESOLVED_SET = OrderedSet<PsiElementResolveResult>()

data class TolkInferenceResult(
    val exprTypes: Map<TolkExpression, TolkTy>,
    val resolvedRefs: Map<TolkReferenceExpression, OrderedSet<PsiElementResolveResult>>
) : TolkInferenceData {
    val timestamp = System.nanoTime()

    override fun getExprTy(expr: TolkExpression): TolkTy =
        exprTypes[expr] ?: TolkTyUnknown

    override fun getResolvedRefs(element: TolkReferenceExpression): OrderedSet<PsiElementResolveResult> {
        return resolvedRefs[element] ?: EMPTY_RESOLVED_SET
    }

    companion object {
    }
}

class TolkInferenceContext(
    val project: Project,
) : TolkInferenceData {
    private val exprTypes = HashMap<TolkExpression, TolkTy>()
    private val resolvedRefs = HashMap<TolkReferenceExpression, OrderedSet<PsiElementResolveResult>>()
    private val diagnostics = ArrayList<TolkDiagnostic>()

    override fun getExprTy(expr: TolkExpression): TolkTy =
        exprTypes[expr] ?: TolkTyUnknown

    fun setExprTy(expr: TolkExpression, ty: TolkTy) {
        exprTypes[expr] = ty
    }

    override fun getResolvedRefs(element: TolkReferenceExpression): OrderedSet<PsiElementResolveResult> {
        return resolvedRefs[element] ?: EMPTY_RESOLVED_SET
    }

    fun setResolvedRefs(element: TolkReferenceExpression, refs: Collection<PsiElementResolveResult>) {
        resolvedRefs[element] = OrderedSet(refs)
    }

    fun isTypeInferred(expression: TolkExpression): Boolean {
        return exprTypes.containsKey(expression)
    }

    fun addDiagnostic(diagnostic: TolkDiagnostic) {
        if (diagnostic.element.containingFile.isPhysical) {
            diagnostics.add(diagnostic)
        }
    }

    fun <T : TolkTyFoldable<T>> resolveTypeVarsIfPossible(ty: T): T {
        TODO("Not yet implemented")
    }

    fun infer(element: TolkInferenceContextOwner): TolkInferenceResult {
        when (element) {
            is TolkFunction -> {
                val walker = TolkInferenceWalker(this)
                val tolkFile = element.containingFile as? TolkFile


                tolkFile?.let {
                    val commonStdlib =
                        TolkIncludeDefinitionMixin.resolveTolkImport(element.project, tolkFile, "@stdlib/common")
                    if (commonStdlib != null) {
                        val tolkCommonStdlib = commonStdlib.findPsiFile(element.project) as? TolkFile
                        if (tolkCommonStdlib != null && tolkFile != tolkCommonStdlib) {
                            walker.inferFile(tolkCommonStdlib, false)
                        }
                    }
                    walker.inferFile(tolkFile)
                }
                walker.inferFunction(element)
            }
        }

        return TolkInferenceResult(exprTypes, resolvedRefs)
    }
}

class TolkInferenceWalker(
    val ctx: TolkInferenceContext,
    val parent: TolkInferenceWalker? = null,
) {
    private val symbolDefinitions = HashMap<String, PsiElement>()

    fun inferFile(element: TolkFile, useIncludes: Boolean = true) {
        val project = element.project
        element.functions.forEach { function ->
            symbolDefinitions[function.name?.removeSurrounding("`") ?: return@forEach] = function
        }
        element.globalVars.forEach { globalVar ->
            symbolDefinitions[globalVar.name?.removeSurrounding("`") ?: return@forEach] = globalVar
        }
        element.constVars.forEach { constVar ->
            symbolDefinitions[constVar.name?.removeSurrounding("`") ?: return@forEach] = constVar
        }
        element.includeDefinitions.forEach {
            val resolvedFile = it.resolveFile(project)
            if (resolvedFile != null) {
                val resolvedTolkFile = resolvedFile.findPsiFile(element.project) as? TolkFile
                if (resolvedTolkFile != null) {
                    inferFile(resolvedTolkFile, false)
                }
            }
        }
    }

    fun inferFunction(element: TolkFunction) {
        element.typeParameterList.forEach { typeParameter ->
            symbolDefinitions[typeParameter.name ?: return@forEach] = typeParameter
        }
        element.functionParameterList.forEach { functionParameter ->
            symbolDefinitions[functionParameter.name?.removeSurrounding("`") ?: return@forEach] = functionParameter
        }
        element.blockStatement?.let {
            TolkInferenceWalker(ctx, this).infer(it)
        }
    }

    fun infer(element: TolkCatch) {
        element.catchParameterList.forEach { catchParameter ->
            symbolDefinitions[catchParameter.identifier.text?.removeSurrounding("`") ?: return@forEach] = catchParameter
        }
        element.blockStatement?.let { blockStatement ->
            infer(blockStatement)
        }
    }

    fun infer(element: TolkVarDefinition) {
        element.varTuple?.varDefinitionList?.forEach {
            infer(it)
        }
        element.varTensor?.varDefinitionList?.forEach {
            infer(it)
        }
        element.`var`?.let {
            symbolDefinitions[it.name?.removeSurrounding("`") ?: return] = it
        }
    }

    private fun infer(element: TolkBlockStatement) {
        element.statementList.forEach { statement ->
            infer(statement)
        }
    }

    private fun infer(element: TolkStatement) {
        when (element) {
            is TolkReturnStatement -> infer(element)
            is TolkBlockStatement -> TolkInferenceWalker(ctx, this).infer(element)
            is TolkRepeatStatement -> infer(element)
            is TolkIfStatement -> infer(element)
            is TolkDoStatement -> infer(element)
            is TolkWhileStatement -> infer(element)
            is TolkAssertStatement -> infer(element)
            is TolkThrowStatement -> infer(element)
            is TolkTryStatement -> infer(element)
            is TolkVarStatement -> infer(element)
            is TolkExpressionStatement -> infer(element)
            else -> {}
        }
    }

    private fun infer(element: TolkReturnStatement) {
        element.expression?.let { expression ->
            infer(expression)
        }
    }

    private fun infer(element: TolkRepeatStatement) {
        element.expression?.let { expression ->
            infer(expression)
        }
        element.blockStatement?.let { blockStatement ->
            TolkInferenceWalker(ctx, this).infer(blockStatement)
        }
    }

    private fun infer(element: TolkIfStatement) {
        element.condition?.let { condition ->
            infer(condition)
        }
        element.blockStatement?.let { blockStatement ->
            TolkInferenceWalker(ctx, this).infer(blockStatement)
        }
        element.elseBranch?.statement?.let { statement ->
            infer(statement)
        }
    }

    private fun infer(element: TolkDoStatement) {
        element.blockStatement?.let { blockStatement ->
            val walker = TolkInferenceWalker(ctx, this)
            walker.infer(blockStatement)
            element.expression?.let { expression ->
                walker.infer(expression)
            }
        }
    }

    private fun infer(element: TolkWhileStatement) {
        element.condition?.let { condition ->
            infer(condition)
        }
        element.blockStatement?.let { blockStatement ->
            TolkInferenceWalker(ctx, this).infer(blockStatement)
        }
    }

    private fun infer(element: TolkAssertStatement) {
        element.assertCondition?.expression?.let { expression ->
            infer(expression)
        }
        element.assertExcNo?.expression?.let { excNo ->
            infer(excNo)
        }
        element.expression?.let { expression ->
            infer(expression)
        }
    }


    private fun infer(element: TolkThrowStatement) {
        element.expression?.let { expression ->
            infer(expression)
        }
    }

    private fun infer(element: TolkTryStatement) {
        element.blockStatement?.let { blockStatement ->
            TolkInferenceWalker(ctx, this).infer(blockStatement)
        }
        element.catch?.let { catch ->
            TolkInferenceWalker(ctx, this).infer(catch)
        }
    }

    private fun infer(element: TolkVarStatement) {
        element.expression?.let { expression ->
            infer(expression)
        }
        element.varDefinition?.let { definition ->
            infer(definition)
        }
    }

    private fun infer(element: TolkExpressionStatement) {
        infer(element.expression)
    }

    private fun infer(element: TolkExpression) {
        when (element) {
            is TolkBinExpression -> infer(element)
            is TolkTernaryExpression -> infer(element)
            is TolkPrefixExpression -> infer(element)
            is TolkDotExpression -> infer(element)
            is TolkCallExpression -> infer(element)
            is TolkTupleExpression -> infer(element)
            is TolkParenExpression -> infer(element)
            is TolkTensorExpression -> infer(element)
            is TolkReferenceExpression -> infer(element)
        }
    }

    private fun infer(element: TolkBinExpression) {
        element.right?.let { expression ->
            infer(expression)
        }
        infer(element.left)
    }

    private fun infer(element: TolkTernaryExpression) {
        element.condition.let { condition ->
            infer(condition)
        }
        element.thenBranch?.let { branch ->
            infer(branch)
        }
        element.elseBranch?.let { branch ->
            infer(branch)
        }
    }

    private fun infer(element: TolkPrefixExpression) {
        element.expression?.let { expression ->
            infer(expression)
        }
    }

    private fun infer(element: TolkDotExpression) {
        infer(element.left)
        element.right?.let { expression ->
            infer(expression)
        }
    }

    private fun infer(element: TolkCallExpression) {
        infer(element.expression)
        element.argumentList.argumentList.forEach { argument ->
            infer(argument.expression)
        }
    }

    private fun infer(element: TolkParenExpression) {
        element.expression?.let { expression ->
            infer(expression)
        }
    }

    private fun infer(element: TolkTensorExpression) {
        element.expressionList.forEach { expression ->
            infer(expression)
        }
    }

    private fun infer(element: TolkTupleExpression) {
        element.expressionList.forEach { expression ->
            infer(expression)
        }
    }

    private fun infer(element: TolkReferenceExpression) {
        val name = element.name?.removeSurrounding("`") ?: return

        var scope: TolkInferenceWalker? = this
        var found: PsiElement? = null
        while (found == null && scope != null) {
            found = scope.symbolDefinitions[name]
            scope = scope.parent
        }

        if (found != null) {
            ctx.setResolvedRefs(element, listOf(PsiElementResolveResult(found)))
        }
    }
}
