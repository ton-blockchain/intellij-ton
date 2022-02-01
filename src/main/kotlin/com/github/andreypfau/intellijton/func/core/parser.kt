package com.github.andreypfau.intellijton.func.core

import com.github.andreypfau.intellijton.func.FuncLanguage
import com.github.andreypfau.intellijton.func.parser.FuncLexerAdapter
import com.github.andreypfau.intellijton.func.parser.FuncParser
import com.github.andreypfau.intellijton.func.psi.FuncFile
import com.github.andreypfau.intellijton.func.psi.FuncTypes
import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiParser
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet

class FuncParserDefinition : ParserDefinition {
    override fun createLexer(project: Project?): Lexer = FuncLexerAdapter()
    override fun createParser(project: Project?): PsiParser = FuncParser()
    override fun getFileNodeType(): IFileElementType = FILE
    override fun getCommentTokens(): TokenSet = COMMENTS
    override fun getStringLiteralElements(): TokenSet = TokenSet.EMPTY
    override fun createElement(node: ASTNode?): PsiElement = FuncTypes.Factory.createElement(node)
    override fun createFile(viewProvider: FileViewProvider): PsiFile = FuncFile(viewProvider)

    companion object {
        val COMMENTS = TokenSet.create(FuncTypes.COMMENT)
        val FILE = IFileElementType(FuncLanguage)
    }
}