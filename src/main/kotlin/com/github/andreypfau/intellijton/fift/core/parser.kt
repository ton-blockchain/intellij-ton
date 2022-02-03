package com.github.andreypfau.intellijton.fift.core

import com.github.andreypfau.intellijton.fift.FiftLanguage
import com.github.andreypfau.intellijton.fift.parser.FiftLexerAdapter
import com.github.andreypfau.intellijton.fift.parser.FiftParser
import com.github.andreypfau.intellijton.fift.psi.FiftFile
import com.github.andreypfau.intellijton.fift.psi.FiftTypes
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

class FiftParserDefinition : ParserDefinition {
    override fun createLexer(project: Project?): Lexer = FiftLexerAdapter()
    override fun createParser(project: Project?): PsiParser = FiftParser()
    override fun getFileNodeType(): IFileElementType = FILE
    override fun getCommentTokens(): TokenSet = COMMENTS
    override fun getStringLiteralElements(): TokenSet = TokenSet.EMPTY
    override fun createElement(node: ASTNode?): PsiElement = FiftTypes.Factory.createElement(node)
    override fun createFile(viewProvider: FileViewProvider): PsiFile = FiftFile(viewProvider)

    companion object {
        val COMMENTS = TokenSet.create(FiftTypes.COMMENT)
        val FILE = IFileElementType(FiftLanguage)
    }
}