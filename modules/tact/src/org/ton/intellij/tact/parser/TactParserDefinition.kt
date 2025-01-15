package org.ton.intellij.tact.parser

import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet
import org.ton.intellij.tact.lexer.TactLexer
import org.ton.intellij.tact.psi.TACT_COMMENTS
import org.ton.intellij.tact.psi.TACT_STRING_LITERALS
import org.ton.intellij.tact.psi.TactElementTypes
import org.ton.intellij.tact.psi.TactFile
import org.ton.intellij.tact.stub.TactFileStub

class TactParserDefinition : ParserDefinition {
    override fun createLexer(project: Project) = TactLexer()

    override fun createParser(project: Project?) = TactParser()

    override fun getFileNodeType() = TactFileStub.Type

    override fun getCommentTokens(): TokenSet = TACT_COMMENTS

    override fun getStringLiteralElements(): TokenSet = TACT_STRING_LITERALS

    override fun createElement(node: ASTNode): PsiElement = TactElementTypes.Factory.createElement(node)

    override fun createFile(viewProvider: FileViewProvider) = TactFile(viewProvider)
}

