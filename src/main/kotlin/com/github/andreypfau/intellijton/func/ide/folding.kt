package com.github.andreypfau.intellijton.func.ide

import com.github.andreypfau.intellijton.collectElements
import com.github.andreypfau.intellijton.func.psi.FuncBlockStatement
import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.FoldingGroup
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElement

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