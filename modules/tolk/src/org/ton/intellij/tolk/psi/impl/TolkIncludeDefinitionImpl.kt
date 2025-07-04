package org.ton.intellij.tolk.psi.impl

import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.lang.ASTNode
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findFile
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.tree.IElementType
import org.ton.intellij.tolk.ide.configurable.tolkSettings
import org.ton.intellij.tolk.psi.TolkFile
import org.ton.intellij.tolk.psi.TolkIncludeDefinition
import org.ton.intellij.tolk.psi.impl.TolkIncludeDefinitionMixin.Companion.resolveTolkImport
import org.ton.intellij.tolk.stub.TolkIncludeDefinitionStub
import org.ton.intellij.util.greenStub

abstract class TolkIncludeDefinitionMixin : StubBasedPsiElementBase<TolkIncludeDefinitionStub>, TolkIncludeDefinition {
    constructor(stub: TolkIncludeDefinitionStub, type: IStubElementType<*, *>) : super(stub, type)
    constructor(node: ASTNode) : super(node)
    constructor(stub: TolkIncludeDefinitionStub?, type: IElementType?, node: ASTNode?) : super(stub, type, node)

    override fun getTextOffset(): Int {
        val stringLiteral = stringLiteral
        return if (stringLiteral != null) {
            stringLiteral.startOffsetInParent + (stringLiteral.rawString?.startOffsetInParent
                ?: return super.getTextOffset())
        } else {
            super.getTextOffset()
        }
    }

    fun resolve(): PsiElement? {
        return stringLiteral?.references?.lastOrNull()?.resolve()
    }

    companion object {
        @Deprecated("Use reference directly")
        fun resolveTolkImport(project: Project, file: TolkFile, path: String): VirtualFile? {
            var path = path
            if (!path.endsWith(".tolk")) {
                path = "$path.tolk"
            }
            if (path.isEmpty()) return null
            return if (path.startsWith("@stdlib/")) {
                val stdlibDir = project.tolkSettings.toolchain?.stdlibDir ?: return null
                val subPath = path.substringAfter("@stdlib/")
                stdlibDir.findFile(subPath)
            } else {
                file.originalFile.virtualFile?.findFileByRelativePath("../$path")
            }
        }
    }
}

val TolkIncludeDefinition.path: String
    get() = greenStub?.path ?: stringLiteral?.rawString?.text ?: ""

@Deprecated("Use reference directly")
fun TolkIncludeDefinition.resolveFile(project: Project) = resolveTolkImport(project, containingFile as TolkFile, path)

fun TolkIncludeDefinition.resolve() = (this as TolkIncludeDefinitionMixin).resolve()
