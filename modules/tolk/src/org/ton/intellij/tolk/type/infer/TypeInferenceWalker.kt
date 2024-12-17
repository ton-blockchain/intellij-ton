package org.ton.intellij.tolk.type.infer

import org.ton.intellij.tolk.psi.TolkNamedElement
import org.ton.intellij.tolk.type.ty.TolkTy

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
