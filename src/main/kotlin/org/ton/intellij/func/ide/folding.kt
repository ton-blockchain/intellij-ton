package org.ton.intellij.func.ide

import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.FoldingGroup
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElement
import org.ton.intellij.collectElements
import org.ton.intellij.func.psi.FuncBlockStatement

class FuncFoldingBuilder : FoldingBuilderEx(), DumbAware {
    override fun buildFoldRegions(root: PsiElement, document: Document, quick: Boolean): Array<FoldingDescriptor> {
        val blockStatementSequence = root.collectElements<FuncBlockStatement>()
        return blockStatementSequence.map { blockStatement ->
            val group = FoldingGroup.newGroup("Folding $blockStatement")
            FoldingDescriptor(
                blockStatement.node,
                blockStatement.textRange,
                group
            )
        }.toTypedArray()
    }

    override fun getPlaceholderText(node: ASTNode): String = "{...}"
    override fun isCollapsedByDefault(node: ASTNode): Boolean = false
}