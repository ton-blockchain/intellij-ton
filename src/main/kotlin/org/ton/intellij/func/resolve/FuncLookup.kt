package org.ton.intellij.func.resolve

import com.intellij.openapi.project.Project
import org.ton.intellij.func.psi.FuncElement
import org.ton.intellij.func.psi.FuncFunction
import org.ton.intellij.func.psi.FuncNamedElement
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

    fun resolve(element: FuncNamedElement): FuncNamedElement? {
        val name = element.identifier?.text ?: return null
        return resolve(name)
    }

    fun resolve(name: String): FuncNamedElement? = definitions[name]
}
