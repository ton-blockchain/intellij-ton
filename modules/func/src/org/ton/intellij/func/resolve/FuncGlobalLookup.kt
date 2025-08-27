package org.ton.intellij.func.resolve

import com.intellij.openapi.project.Project
import org.ton.intellij.func.psi.*

class FuncGlobalLookup(project: Project) {
    private val definitions = HashMap<String, FuncNamedElement>()

    init {
        FuncPsiFactory[project].builtinFile.functions.forEach { function ->
            val name = function.name ?: return@forEach
            definitions[name] = function
        }
    }

    fun resolve(element: FuncNamedElement): Collection<FuncNamedElement>? {
        val name = element.identifier?.text ?: return null
        val parent = element.parent
        if (parent is FuncApplyExpression && parent.left == element) {
            val grandParent = parent.parent
            if (grandParent is FuncSpecialApplyExpression && grandParent.right == parent) {
                if (name.startsWith('.')) {
                    return resolve(name.substring(1))
                }
                if (name.startsWith('~')) {
                    return resolve(name) ?: resolve(name.substring(1))
                }
            }
        }
        return resolve(name)
    }

    fun resolve(name: String): Collection<FuncNamedElement>? = definitions[name]?.let { listOf(it) }
}
