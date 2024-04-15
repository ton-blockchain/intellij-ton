package org.ton.intellij.tact.type

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.util.containers.OrderedSet
import org.ton.intellij.tact.diagnostics.TactDiagnostic
import org.ton.intellij.tact.psi.TactExpression
import org.ton.intellij.tact.psi.TactInferenceContextOwner
import org.ton.intellij.util.recursionGuard

private val TACT_INFERENCE_KEY: Key<CachedValue<TactInferenceResult>> = Key.create("TACT_INFERENCE_KEY")

val TactInferenceContextOwner.selfInferenceResult: TactInferenceResult
    get() {
        return CachedValuesManager.getCachedValue(this, TACT_INFERENCE_KEY) {
            val inferred = inferTypesIn(this)
            CachedValueProvider.Result.create(inferred, PsiModificationTracker.MODIFICATION_COUNT)
        }
    }

fun inferTypesIn(element: TactInferenceContextOwner): TactInferenceResult {
    val lookup = TactLookup(element.project, element)
    return recursionGuard(element, { lookup.ctx.infer(element) }, memoize = false)
        ?: error("Can not run nested type inference")
}

interface TactInferenceData {
    val diagnostics: List<TactDiagnostic>

    fun getExprTy(expr: TactExpression): TactTy?

    fun getResolvedRefs(element: PsiElement): OrderedSet<PsiElementResolveResult>
}

data class TactInferenceResult(
    val exprTypes: Map<TactExpression, TactTy>,
    val resolvedRefs: Map<PsiElement, OrderedSet<PsiElementResolveResult>>,
    override val diagnostics: List<TactDiagnostic> = emptyList()
) : TactInferenceData {
    val timestamp = System.nanoTime()

    override fun getExprTy(expr: TactExpression): TactTy =
        exprTypes[expr] ?: TactTyUnknown

    override fun getResolvedRefs(element: PsiElement): OrderedSet<PsiElementResolveResult> {
        return resolvedRefs[element] ?: EMPTY_RESOLVED_SET
    }
}


private val EMPTY_RESOLVED_SET = OrderedSet<PsiElementResolveResult>()

class TactInferenceContext(
    val project: Project,
    val lookup: TactLookup
) : TactInferenceData {
    private val resolvedTypes = HashMap<String, TactTy>()
    private val exprTypes = HashMap<TactExpression, TactTy>()
    private val resolvedRefs = HashMap<PsiElement, OrderedSet<PsiElementResolveResult>>()
    override val diagnostics = ArrayList<TactDiagnostic>()

    override fun getExprTy(expr: TactExpression): TactTy? {
        return exprTypes[expr]
    }

    fun setExprTy(expr: TactExpression, ty: TactTy) {
        exprTypes[expr] = ty
    }

    fun isTypeInferred(expr: TactExpression): Boolean {
        return exprTypes.containsKey(expr)
    }

    override fun getResolvedRefs(element: PsiElement): OrderedSet<PsiElementResolveResult> {
        return resolvedRefs[element] ?: EMPTY_RESOLVED_SET
    }

    fun setResolvedRefs(element: PsiElement, refs: OrderedSet<PsiElementResolveResult>) {
        resolvedRefs[element] = refs
    }

    fun addDiagnostic(diagnostic: TactDiagnostic) {
        if (diagnostic.element.containingFile.isPhysical) {
            diagnostics.add(diagnostic)
        }
    }

    fun reportTypeMismatch(element: PsiElement, expected: TactTy, actual: TactTy) {
        addDiagnostic(TactDiagnostic.TypeError(element, expected, actual))
    }

    fun infer(element: TactInferenceContextOwner): TactInferenceResult {
        element.body?.let { block ->
            val walker = TactTypeInferenceWalker(this, element.selfType ?: TactTyUnknown)
            walker.walk(block)
        }
        return TactInferenceResult(exprTypes, resolvedRefs, diagnostics)
    }
}
