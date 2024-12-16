package org.ton.intellij.tolk.type.infer

import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiElementResolveResult
import org.ton.intellij.tolk.psi.*
import org.ton.intellij.tolk.type.ty.*
import org.ton.intellij.util.infiniteWith

class TolkTypeInferenceWalker(
    val ctx: TolkInferenceContext,
    private val returnTy: TolkTy
) {
    private var variableDeclarationState = false
    private val definedVariables = ArrayDeque<HashMap<String, MutableList<TolkNamedElement>>>()

    private fun define(vararg namedElements: TolkNamedElement) {
        definedVariables.firstOrNull()?.let {
            val name = namedElements.firstOrNull()?.name?.removeSurrounding("`") ?: return
            it.getOrPut(name) {
                ArrayList()
            }.addAll(namedElements)
        }
    }

    private fun resolve(name: String): List<TolkNamedElement>? {
        val formatted = name.removeSurrounding("`")
        definedVariables.forEach {
            val variable = it[formatted] ?: return@forEach
            return variable
        }
        return null
    }
}
