package org.ton.intellij.tasm.parser

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
import org.ton.intellij.tasm.TasmFileElementType
import org.ton.intellij.tasm.lexer.TasmLexerAdapter
import org.ton.intellij.tasm.psi.TasmFile
import org.ton.intellij.tasm.psi.TasmTypes
import org.ton.intellij.util.tokenSetOf

class TasmParserDefinition : ParserDefinition {
    override fun createLexer(project: Project?): Lexer = TasmLexerAdapter()
    override fun createParser(project: Project?): PsiParser = TasmParser()
    override fun getFileNodeType(): IFileElementType = TasmFileElementType
    override fun getCommentTokens(): TokenSet = TASM_COMMENTS
    override fun getStringLiteralElements(): TokenSet = tokenSetOf()
    override fun createElement(node: ASTNode?): PsiElement = TasmTypes.Factory.createElement(node)
    override fun createFile(viewProvider: FileViewProvider): PsiFile = TasmFile(viewProvider)
}

val TASM_COMMENTS = tokenSetOf(TasmTypes.COMMENT)
val TASM_KEYWORDS = tokenSetOf(
    TasmTypes.REF,
    TasmTypes.EMBED,
    TasmTypes.EXOTIC,
    TasmTypes.LIBRARY,
)
val TASM_BRACES = tokenSetOf(
    TasmTypes.LBRACE,
    TasmTypes.RBRACE,
)
val TASM_BRACKETS = tokenSetOf(
    TasmTypes.LBRACKET,
    TasmTypes.RBRACKET,
)
