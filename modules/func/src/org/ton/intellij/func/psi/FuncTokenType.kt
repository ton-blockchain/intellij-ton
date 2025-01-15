package org.ton.intellij.func.psi

import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import org.ton.intellij.func.FuncLanguage
import org.ton.intellij.func.parser.FuncParserDefinition.Companion.BLOCK_COMMENT
import org.ton.intellij.func.parser.FuncParserDefinition.Companion.BLOCK_DOC_COMMENT
import org.ton.intellij.func.parser.FuncParserDefinition.Companion.EOL_COMMENT
import org.ton.intellij.func.parser.FuncParserDefinition.Companion.EOL_DOC_COMMENT
import org.ton.intellij.util.tokenSetOf

open class FuncTokenType(val name: String) : IElementType(name, FuncLanguage)

val FUNC_REGULAR_COMMENTS get() = tokenSetOf(BLOCK_COMMENT, EOL_COMMENT)
val FUNC_DOC_COMMENTS get() = tokenSetOf(EOL_DOC_COMMENT, BLOCK_DOC_COMMENT)
val FUNC_COMMENTS get() = TokenSet.orSet(FUNC_REGULAR_COMMENTS, FUNC_DOC_COMMENTS)

val FUNC_KEYWORDS = tokenSetOf(
    FuncElementTypes.RETURN_KEYWORD,
    FuncElementTypes.VAR_KEYWORD,
    FuncElementTypes.REPEAT_KEYWORD,
    FuncElementTypes.DO_KEYWORD,
    FuncElementTypes.WHILE_KEYWORD,
    FuncElementTypes.UNTIL_KEYWORD,
    FuncElementTypes.TRY_KEYWORD,
    FuncElementTypes.CATCH_KEYWORD,
    FuncElementTypes.IF_KEYWORD,
    FuncElementTypes.IFNOT_KEYWORD,
    FuncElementTypes.ELSE_KEYWORD,
    FuncElementTypes.ELSEIF_KEYWORD,
    FuncElementTypes.ELSEIFNOT_KEYWORD,

    FuncElementTypes.TYPE_KEYWORD,
    FuncElementTypes.FORALL_KEYWORD,

    FuncElementTypes.EXTERN_KEYWORD,
    FuncElementTypes.GLOBAL_KEYWORD,
    FuncElementTypes.ASM_KEYWORD,
    FuncElementTypes.IMPURE_KEYWORD,
    FuncElementTypes.INLINE_KEYWORD,
    FuncElementTypes.INLINE_REF_KEYWORD,
    FuncElementTypes.AUTO_APPLY_KEYWORD,
    FuncElementTypes.METHOD_ID_KEYWORD,
    FuncElementTypes.OPERATOR_KEYWORD,
    FuncElementTypes.INFIX_KEYWORD,
    FuncElementTypes.INFIXL_KEYWORD,
    FuncElementTypes.INFIXR_KEYWORD,
    FuncElementTypes.CONST_KEYWORD,
)
