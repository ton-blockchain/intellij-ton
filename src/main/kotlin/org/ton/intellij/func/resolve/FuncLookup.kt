package org.ton.intellij.func.resolve

import com.intellij.openapi.project.Project
import org.ton.intellij.func.psi.*
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
//            if (!name.startsWith('~')) {
//                definitions[".$name"] = function
//            }
        }
        if (context is FuncFunction) {
            context.functionParameterList.forEach {
                define(it)
            }
        }
    }

//    private fun processFile(context: FuncElement, file: FuncFile) {
////        println("LOOKUP: processing file: $file by $context")
//        file.collectIncludedFiles().toList().also {
////            println("Collected included files: $it")
//        }.forEach {
//            for (constant in it.constVars) {
//                define(constant)
//            }
//            for (globalVar in it.globalVars) {
//                define(globalVar)
//            }
//            for (function in it.functions) {
////                println("defining: $function")
//                define(function)
//                if (function == context) {
//                    return
//                }
//            }
//        }
//        println("===============")
//    }

    fun define(element: FuncNamedElement) {
        val name = element.name ?: return
        definitions[name] = element
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
