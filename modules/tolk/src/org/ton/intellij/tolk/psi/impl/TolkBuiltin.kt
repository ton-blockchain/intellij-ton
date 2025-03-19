package org.ton.intellij.tolk.psi.impl

import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener
import com.intellij.openapi.startup.ProjectActivity
import org.ton.intellij.tolk.psi.TolkFile
import org.ton.intellij.tolk.psi.TolkFunction
import org.ton.intellij.tolk.psi.TolkPsiFactory

@Service(Service.Level.PROJECT)
class TolkBuiltins(
    val project: Project,
) {
    private var file: TolkFile? = null
    private val functions = mutableMapOf<String, TolkFunction>()

    private fun clear() {
        functions.clear()
        file = null
    }

    private fun registerBuiltin() {
        val fileName = "builtin.tolk"
        val text = TolkPsiFactory::class.java.classLoader.getResourceAsStream("tolk/builtin.tolk").use { stream ->
            stream?.readAllBytes()?.decodeToString()
        } ?: ""
        val file = TolkPsiFactory[project].createFile(fileName, text).also {
            file = it
        }
        val virtualFile = file.virtualFile
        virtualFile.isWritable = false

        file.functions.asSequence().mapNotNull {
            it.name?.let { name -> name to it }
        }.toMap(functions)
    }

    fun getFunction(name: String): TolkFunction? = functions[name]

    companion object {
        operator fun get(project: Project): TolkBuiltins = project.getService(TolkBuiltins::class.java)
    }

    class UnregisterListener : ProjectManagerListener {
        override fun projectClosed(project: Project) {
            TolkBuiltins[project].clear()
        }
    }

    class RegisterActivity : ProjectActivity, DumbAware {
        override suspend fun execute(project: Project) {
            invokeLater {
                TolkBuiltins[project].registerBuiltin()
            }
        }
    }
}
