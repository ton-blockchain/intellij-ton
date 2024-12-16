package org.ton.intellij.asm.parser

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
import org.ton.intellij.asm.AsmFile
import org.ton.intellij.asm.AsmFileElementType
import org.ton.intellij.asm.AsmLanguage
import org.ton.intellij.asm.lexer.AsmLexer
import org.ton.intellij.asm.psi.AsmElementTypes

class AsmParserDefinition : ParserDefinition {
    override fun createLexer(project: Project?): Lexer {
        return AsmLexer()
    }

    override fun createParser(project: Project?): PsiParser {
        return AsmParser()
    }

    override fun getFileNodeType(): IFileElementType = AsmFileElementType

    override fun getCommentTokens(): TokenSet = TokenSet.EMPTY

    override fun getStringLiteralElements(): TokenSet = TokenSet.EMPTY

    override fun createElement(node: ASTNode?): PsiElement {
        return AsmElementTypes.Factory.createElement(node)
    }

    override fun createFile(viewProvider: FileViewProvider): PsiFile {
        return AsmFile(viewProvider, AsmLanguage)
    }
}