package org.ton.intellij.tolk.coverage

import com.intellij.coverage.BaseCoverageSuite
import com.intellij.coverage.CoverageEngine
import com.intellij.coverage.CoverageFileProvider
import com.intellij.coverage.CoverageRunner
import com.intellij.openapi.project.Project
import org.jdom.Element

class TolkCoverageSuite : BaseCoverageSuite {
    var contextFilePath: String? private set

    constructor() : super() {
        contextFilePath = null
    }

    constructor(
        project: Project,
        name: String,
        fileProvider: CoverageFileProvider,
        coverageRunner: CoverageRunner,
        contextFilePath: String?,
    ) : super(name, fileProvider, System.currentTimeMillis(), false, false, false, coverageRunner, project) {
        this.contextFilePath = contextFilePath
    }

    override fun getCoverageEngine(): CoverageEngine = TolkCoverageEngine.getInstance()

    override fun writeExternal(element: Element) {
        super.writeExternal(element)
        element.setAttribute(CONTEXT_FILE_PATH, contextFilePath ?: return)
    }

    override fun readExternal(element: Element) {
        super.readExternal(element)
        contextFilePath = element.getAttributeValue(CONTEXT_FILE_PATH) ?: return
    }

    companion object {
        private const val CONTEXT_FILE_PATH: String = "CONTEXT_FILE_PATH"
    }
}
