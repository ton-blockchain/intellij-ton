package org.ton.intellij.func.parser

import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiParser
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet
import org.ton.intellij.func.FuncFileElementType
import org.ton.intellij.func.lexer.FuncLexer
import org.ton.intellij.func.psi.FuncElementTypes
import org.ton.intellij.func.psi.FuncFile

class FuncParserDefinition : ParserDefinition {
    override fun createLexer(project: Project?): Lexer = FuncLexer()

    override fun createParser(project: Project?): PsiParser = FuncParser()

    override fun getFileNodeType(): IFileElementType = FuncFileElementType

    override fun getCommentTokens(): TokenSet = COMMENTS

    override fun getStringLiteralElements(): TokenSet = STRING_LITERALS

    override fun createElement(node: ASTNode?): PsiElement =
        FuncElementTypes.Factory.createElement(node)

    override fun createFile(viewProvider: FileViewProvider) =
        FuncFile(viewProvider)

    companion object {
        val MACRO = TokenSet.create(FuncElementTypes.INCLUDE)
        val COMMENTS = TokenSet.create(FuncElementTypes.BLOCK_COMMENT)
        val STRING_LITERALS =
            TokenSet.create(FuncElementTypes.RAW_STRING, FuncElementTypes.QUOTE, FuncElementTypes.STRING_LITERAL)
    }
}
