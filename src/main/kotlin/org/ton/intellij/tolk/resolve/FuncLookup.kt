package org.ton.intellij.tolk.resolve

import com.intellij.openapi.project.Project
import org.ton.intellij.tolk.psi.*
import org.ton.intellij.tolk.type.infer.TolkInferenceContext

class TolkLookup(
    private val project: Project,
    context: TolkElement? = null
) {
    val ctx by lazy(LazyThreadSafetyMode.NONE) {
        TolkInferenceContext(project, this)
    }

    private val definitions = HashMap<String, TolkNamedElement>()

    init {
//        TolkPsiFactory[project].builtinFile.functions.forEach { function ->
//            val name = function.name ?: return@forEach
//            definitions[name] = function
//        }
        if (context is TolkFunction) {
            context.functionParameterList.forEach {
                define(it)
            }
        }
    }

    fun define(element: TolkNamedElement) {
        val name = element.name ?: return
        definitions[name.removeSurrounding("`")] = element
    }

    fun resolve(element: TolkNamedElement): Collection<TolkNamedElement>? {
        val name = element.identifier?.text ?: return null
        val parent = element.parent
        // TODO: fix
//        if (parent is TolkApplyExpression && parent.left == element) {
//            val grandParent = parent.parent
//            if (grandParent is TolkSpecialApplyExpression && grandParent.right == parent) {
//                if (name.startsWith('.')) {
//                    return resolve(name.substring(1))
//                }
//                if (name.startsWith('~')) {
//                    return resolve(name) ?: resolve(name.substring(1))
//                }
//            }
//        }
        return resolve(name)
    }

    private fun resolve(name: String): Collection<TolkNamedElement>? =
        definitions[name.removeSurrounding("`")]?.let { listOf(it) }

//    private fun String.removeBackTicks(): String = if (startsWith('`') && endsWith('`')) {
//        substring(1, length - 1)
//    } else {
//        this
//    }
}
