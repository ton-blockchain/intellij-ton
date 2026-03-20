package org.ton.intellij.acton.profiler

import com.intellij.openapi.project.Project
import com.intellij.profiler.CollapsedDumpParser
import com.intellij.profiler.CollapsedProfilerDumpFileParserBase
import com.intellij.profiler.ValueCallStackElement
import com.intellij.profiler.api.BaseCallStackElement
import com.intellij.profiler.api.ProfilerDumpFileParser
import com.intellij.profiler.api.ProfilerDumpParserProvider
import com.intellij.profiler.ui.BaseCallStackElementRenderer
import com.intellij.profiler.simpleCollapsedDumpParser

class ActonCollapsedProfileDumpParserProvider : ProfilerDumpParserProvider {
    override val id: String
        get() = ID

    override val name: String
        get() = "Acton Collapsed Profile"

    override val requiredFileExtension: String
        get() = FILE_EXTENSION

    override fun createParser(project: Project): ProfilerDumpFileParser = ActonCollapsedProfileDumpParser(project)

    companion object {
        const val ID: String = "org.ton.intellij.acton.collapsed-profile"
        const val FILE_EXTENSION: String = "collapsed"
    }
}

private class ActonCollapsedProfileDumpParser(project: Project) : CollapsedProfilerDumpFileParserBase(project) {
    @Suppress("UNCHECKED_CAST")
    override fun createCollapsedParser(project: Project): CollapsedDumpParser<BaseCallStackElement> {
        return simpleCollapsedDumpParser(::isActonThreadName)
            as CollapsedDumpParser<BaseCallStackElement>
    }

    override fun createStackElementRenderer(): BaseCallStackElementRenderer = ActonCollapsedCallStackElementRenderer

    override val helpId: String
        get() = ""
}

private object ActonCollapsedCallStackElementRenderer : BaseCallStackElementRenderer() {
    override fun getText(node: BaseCallStackElement): String {
        val stackElement = node as? ValueCallStackElement<*> ?: return super.getText(node)
        return stackElement.value as? String ?: stackElement.fullName()
    }
}

private fun isActonThreadName(firstFrame: String): Boolean = firstFrame == "acton"
