package org.ton.intellij.func.type.infer

import com.intellij.openapi.project.Project
import org.ton.intellij.func.psi.FuncExpression
import org.ton.intellij.func.type.ty.FuncTy
import org.ton.intellij.func.type.ty.FuncTyUnknown

interface FuncInferenceData {
    fun getExprTy(expr: FuncExpression): FuncTy
}

class FuncInferenceResult(
    val exprTypes: Map<FuncExpression, FuncTy>
) : FuncInferenceData {
    val timestamp = System.nanoTime()

    override fun getExprTy(expr: FuncExpression): FuncTy =
        exprTypes[expr] ?: FuncTyUnknown
}

class FuncInferenceContext(
    val project: Project
) : FuncInferenceData {
    private val exprTypes = HashMap<FuncExpression, FuncTy>()

    override fun getExprTy(expr: FuncExpression): FuncTy =
        exprTypes[expr] ?: FuncTyUnknown

    fun setExprTy(expr: FuncExpression, ty: FuncTy) {
        exprTypes[expr] = ty
    }

    fun isTypeInferred(expression: FuncExpression): Boolean {
        return exprTypes.containsKey(expression)
    }

    fun <T : FuncTypeFoldable<T>> resolveTypeVarsIfPossible(ty: T): T {
        TODO("Not yet implemented")
    }
}
