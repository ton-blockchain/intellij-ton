package org.ton.intellij.tolk.codeInsight.codeInsight

import org.ton.intellij.tolk.codeInsight.TolkCodeInsightBaseTest
import org.ton.intellij.tolk.ide.configurable.tolkSettings
import java.io.File

class TolkProjectsCodeInsightTest : TolkCodeInsightBaseTest() {
    override fun getTestDataPath(): String = "${super.testDataPath}/projects"

    override fun setUp() {
        super.setUp()
        val file = myFixture.copyDirectoryToProject("../tolk-stdlib", "tolk-stdlib")
        project.tolkSettings.explicitPathToStdlib = file.url
    }

    fun `test tolk bench project`() = doRecursiveTest("tolk-bench")
    fun `test payment channel project`() = doRecursiveTest("payment-channel-contract/contracts")
    fun `test chainlink-ton project`() = doRecursiveTest("chainlink-ton/contracts")

    fun doRecursiveTest(directoryName: String) {
        val testDataDir = File(getTestDataPath(), directoryName)
        require(testDataDir.exists() && testDataDir.isDirectory) {
            "Test directory not found: ${testDataDir.absolutePath}"
        }
        
        val tolkFiles = findTolkFilesRecursively(testDataDir)
        require(tolkFiles.isNotEmpty()) {
            "No .tolk files found in directory: ${testDataDir.absolutePath}"
        }
        
        myFixture.copyDirectoryToProject(directoryName, directoryName)
        
        tolkFiles.forEach { tolkFile ->
            val relativePath = testDataDir.toPath().relativize(tolkFile.toPath()).toString()
            val testPath = "$directoryName/$relativePath"
            try {
                myFixture.testHighlighting(testPath)
            } catch (e: Exception) {
                throw AssertionError("Highlighting test failed for file: $testPath", e)
            }
        }
    }

    private fun findTolkFilesRecursively(directory: File): List<File> {
        val result = mutableListOf<File>()
        
        fun traverse(dir: File) {
            dir.listFiles()?.forEach { file ->
                when {
                    file.isDirectory -> traverse(file)
                    file.extension == "tolk" -> result.add(file)
                }
            }
        }
        
        traverse(directory)
        return result
    }
}
