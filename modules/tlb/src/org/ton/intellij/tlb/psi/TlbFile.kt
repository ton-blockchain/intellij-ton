package org.ton.intellij.tlb.psi

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import org.ton.intellij.tlb.TlbFileType
import org.ton.intellij.tlb.TlbLanguage
import java.io.File

class TlbFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, TlbLanguage), TlbElement {
    override fun getFileType(): FileType = TlbFileType
    override fun toString(): String = "TLB"

    fun findResultTypes(name: String): Sequence<TlbResultType> = sequence {
        val constructors = findChildrenByClass(TlbConstructor::class.java)
        for (constructor in constructors) {
            val resultType = constructor.resultType ?: continue
            if (resultType.name == name) {
                yield(resultType)
            }
        }
    }

    fun resultTypes(): Sequence<TlbResultType> = sequence {
        val constructors = findChildrenByClass(TlbConstructor::class.java)
        for (constructor in constructors) {
            val resultType = constructor.resultType ?: continue
            yield(resultType)
        }
    }

    fun constructors(): List<TlbConstructor> {
        return findChildrenByClass(TlbConstructor::class.java).toList()
    }

    /**
     * Find all dependson declarations in comments like:
     * ```
     * // dependson "path/to/file.tlb"
     * /*
     *  dependson "another/path.tlb"
     * */
     * ```
     */
    fun getDependencies(): List<String> {
        val dependencies = mutableListOf<String>()
        val comments = PsiTreeUtil.findChildrenOfType(this, PsiComment::class.java)

        for (comment in comments) {
            val text = comment.text

            if (text.startsWith("//")) {
                val content = text.substring(2).trim()
                extractDependency(content)?.let { dependencies.add(it) }
            } else if (text.startsWith("/*") && text.endsWith("*/")) {
                val content = text.substring(2, text.length - 2)
                val lines = content.split('\n', '\r')

                for (line in lines) {
                    val trimmedLine = line.trim().removePrefix("*").trim()
                    extractDependency(trimmedLine)?.let { dependencies.add(it) }
                }
            }
        }

        return dependencies
    }

    private fun extractDependency(content: String): String? {
        if (content.startsWith("dependson")) {
            val dependsonContent = content.substring("dependson".length).trim()
            val pathMatch = Regex("\"([^\"]+)\"").find(dependsonContent)
            return pathMatch?.groupValues?.get(1)
        }
        return null
    }

    fun getDependencyFiles(): List<TlbFile> {
        val dependencies = getDependencies()
        val dependencyFiles = mutableListOf<TlbFile>()
        val currentFile = virtualFile ?: return emptyList()
        val psiManager = PsiManager.getInstance(project)

        for (dependencyPath in dependencies) {
            val resolvedFile = if (File(dependencyPath).isAbsolute) {
                VirtualFileManager.getInstance().findFileByUrl("file://$dependencyPath")
            } else {
                currentFile.parent?.findFileByRelativePath(dependencyPath)
            }

            if (resolvedFile != null && resolvedFile.fileType == TlbFileType) {
                val psiFile = psiManager.findFile(resolvedFile) as? TlbFile
                if (psiFile != null) {
                    dependencyFiles.add(psiFile)
                }
            }
        }

        return dependencyFiles
    }
}
