package org.ton.intellij.func.type.infer

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementResolveResult
import com.intellij.util.containers.OrderedSet
import org.ton.intellij.func.diagnostics.FuncDiagnostic
import org.ton.intellij.func.psi.FuncExpression
import org.ton.intellij.func.psi.FuncFunction
import org.ton.intellij.func.psi.FuncInferenceContextOwner
import org.ton.intellij.func.psi.FuncReferenceExpression
import org.ton.intellij.func.resolve.FuncLookup
import org.ton.intellij.func.type.ty.FuncTy
import org.ton.intellij.func.type.ty.FuncTyUnknown
import org.ton.intellij.util.recursionGuard

fun inferTypesIn(element: FuncInferenceContextOwner): FuncInferenceResult {
    val lookup = FuncLookup(element.project, element)
    return recursionGuard(element, memoize = false) { lookup.ctx.infer(element) }
        ?: error("Can not run nested type inference")
}

interface FuncInferenceData {
    fun getExprTy(expr: FuncExpression): FuncTy

    fun getResolvedRefs(element: FuncReferenceExpression): OrderedSet<PsiElementResolveResult>
}

private val EMPTY_RESOLVED_SET = OrderedSet<PsiElementResolveResult>()

data class FuncInferenceResult(
    val exprTypes: Map<FuncExpression, FuncTy>,
    val resolvedRefs: Map<FuncReferenceExpression, OrderedSet<PsiElementResolveResult>>
) : FuncInferenceData {
    val timestamp = System.nanoTime()

    override fun getExprTy(expr: FuncExpression): FuncTy =
        exprTypes[expr] ?: FuncTyUnknown

    override fun getResolvedRefs(element: FuncReferenceExpression): OrderedSet<PsiElementResolveResult> {
        return resolvedRefs[element] ?: EMPTY_RESOLVED_SET
    }

    companion object
}

class FuncInferenceContext(
    val project: Project,
    val lookup: FuncLookup
) : FuncInferenceData {
    private val exprTypes = HashMap<FuncExpression, FuncTy>()
    private val resolvedRefs = HashMap<FuncReferenceExpression, OrderedSet<PsiElementResolveResult>>()
    private val diagnostics = ArrayList<FuncDiagnostic>()

    override fun getExprTy(expr: FuncExpression): FuncTy =
        exprTypes[expr] ?: FuncTyUnknown

    fun setExprTy(expr: FuncExpression, ty: FuncTy) {
        exprTypes[expr] = ty
    }

    override fun getResolvedRefs(element: FuncReferenceExpression): OrderedSet<PsiElementResolveResult> {
        return resolvedRefs[element] ?: EMPTY_RESOLVED_SET
    }

    fun setResolvedRefs(element: FuncReferenceExpression, refs: Collection<PsiElementResolveResult>) {
        resolvedRefs[element] = OrderedSet(refs)
    }

    fun isTypeInferred(expression: FuncExpression): Boolean {
        return exprTypes.containsKey(expression)
    }

    fun addDiagnostic(diagnostic: FuncDiagnostic) {
        if (diagnostic.element.containingFile.isPhysical) {
            diagnostics.add(diagnostic)
        }
    }

    fun <T : FuncTyFoldable<T>> resolveTypeVarsIfPossible(ty: T): T {
        TODO("Not yet implemented")
    }

    fun infer(element: FuncInferenceContextOwner): FuncInferenceResult {
        when (element) {
            is FuncFunction -> {
                val walker = FuncTypeInferenceWalker(this, FuncTyUnknown)
                element.blockStatement?.let {
                    walker.inferFunctionBody(it)
                }
            }
        }

        return FuncInferenceResult(exprTypes, resolvedRefs).also {
//            println("Inferred types: $it")
        }
    }
}
