package org.ton.intellij.func.resolve

import com.intellij.openapi.project.Project
import org.ton.intellij.func.psi.FuncElement
import org.ton.intellij.func.psi.FuncFunction
import org.ton.intellij.func.psi.FuncNamedElement
import org.ton.intellij.func.psi.FuncPsiFactory
import org.ton.intellij.func.type.infer.FuncInferenceContext

class FuncLookup(
    private val project: Project,
    context: FuncElement? = null
) {
    val ctx by lazy(LazyThreadSafetyMode.NONE) {
        FuncInferenceContext(project, this)
    }

    private val definitions = HashMap<String, FuncNamedElement>()

    init {
        FuncPsiFactory[project].builtinStdlibFile.functions.forEach { function ->
            val name = function.name ?: return@forEach
            definitions[name] = function
            if (!name.startsWith('~')) {
                definitions[".$name"] = function
            }
        }
        if (context is FuncFunction) {
            context.functionParameterList.forEach {
                define(it)
            }
        }
    }

    fun define(element: FuncNamedElement) {
        val name = element.identifier?.text ?: return
        definitions[name] = element
    }

    fun resolve(element: FuncNamedElement): Collection<FuncNamedElement>? {
        val name = element.identifier?.text ?: return null
        return resolve(name)
    }

    fun resolve(name: String): Collection<FuncNamedElement>? = definitions[name]?.let { listOf(it) }
}
