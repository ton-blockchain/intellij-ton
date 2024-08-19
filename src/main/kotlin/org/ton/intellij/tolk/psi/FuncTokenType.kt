package org.ton.intellij.tolk.psi

import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import org.ton.intellij.tolk.TolkLanguage
import org.ton.intellij.tolk.parser.TolkParserDefinition.Companion.BLOCK_COMMENT
import org.ton.intellij.tolk.parser.TolkParserDefinition.Companion.BLOCK_DOC_COMMENT
import org.ton.intellij.tolk.parser.TolkParserDefinition.Companion.EOL_COMMENT
import org.ton.intellij.tolk.parser.TolkParserDefinition.Companion.EOL_DOC_COMMENT
import org.ton.intellij.util.tokenSetOf

open class TolkTokenType(val name: String) : IElementType(name, TolkLanguage)

val FUNC_REGULAR_COMMENTS get() = tokenSetOf(BLOCK_COMMENT, EOL_COMMENT)
val FUNC_DOC_COMMENTS get() = tokenSetOf(EOL_DOC_COMMENT, BLOCK_DOC_COMMENT)
val FUNC_COMMENTS get() = TokenSet.orSet(FUNC_REGULAR_COMMENTS, FUNC_DOC_COMMENTS)

val FUNC_KEYWORDS = tokenSetOf(
    TolkElementTypes.RETURN_KEYWORD,
    TolkElementTypes.VAR_KEYWORD,
    TolkElementTypes.REPEAT_KEYWORD,
    TolkElementTypes.DO_KEYWORD,
    TolkElementTypes.WHILE_KEYWORD,
    TolkElementTypes.UNTIL_KEYWORD,
    TolkElementTypes.TRY_KEYWORD,
    TolkElementTypes.CATCH_KEYWORD,
    TolkElementTypes.IF_KEYWORD,
    TolkElementTypes.IFNOT_KEYWORD,
    TolkElementTypes.THEN_KEYWORD,
    TolkElementTypes.ELSE_KEYWORD,
    TolkElementTypes.ELSEIF_KEYWORD,
    TolkElementTypes.ELSEIFNOT_KEYWORD,

    TolkElementTypes.TYPE_KEYWORD,
    TolkElementTypes.FORALL_KEYWORD,

    TolkElementTypes.EXTERN_KEYWORD,
    TolkElementTypes.GLOBAL_KEYWORD,
    TolkElementTypes.ASM_KEYWORD,
    TolkElementTypes.IMPURE_KEYWORD,
    TolkElementTypes.INLINE_KEYWORD,
    TolkElementTypes.INLINE_REF_KEYWORD,
    TolkElementTypes.AUTO_APPLY_KEYWORD,
    TolkElementTypes.METHOD_ID_KEYWORD,
    TolkElementTypes.OPERATOR_KEYWORD,
    TolkElementTypes.INFIX_KEYWORD,
    TolkElementTypes.INFIXL_KEYWORD,
    TolkElementTypes.INFIXR_KEYWORD,
    TolkElementTypes.CONST_KEYWORD,
    TolkElementTypes.PURE_KEYWORD,
    TolkElementTypes.BUILTIN_KEYWORD,
    TolkElementTypes.GET_KEYWORD,
)
