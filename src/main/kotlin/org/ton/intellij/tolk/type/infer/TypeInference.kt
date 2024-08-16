package org.ton.intellij.tolk.type.infer

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementResolveResult
import com.intellij.util.containers.OrderedSet
import org.ton.intellij.tolk.diagnostics.TolkDiagnostic
import org.ton.intellij.tolk.psi.TolkExpression
import org.ton.intellij.tolk.psi.TolkFunction
import org.ton.intellij.tolk.psi.TolkInferenceContextOwner
import org.ton.intellij.tolk.psi.TolkReferenceExpression
import org.ton.intellij.tolk.resolve.TolkLookup
import org.ton.intellij.tolk.type.ty.TolkTy
import org.ton.intellij.tolk.type.ty.TolkTyUnknown
import org.ton.intellij.util.recursionGuard

fun inferTypesIn(element: TolkInferenceContextOwner): TolkInferenceResult {
    val lookup = TolkLookup(element.project, element)
    return recursionGuard(element, memoize = false) { lookup.ctx.infer(element) }
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
    val lookup: TolkLookup
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
                val walker = TolkTypeInferenceWalker(this, TolkTyUnknown)
                element.blockStatement?.let {
                    walker.inferFunctionBody(it)
                }
            }
        }

        return TolkInferenceResult(exprTypes, resolvedRefs).also {
//            println("Inferred types: $it")
        }
    }
}
