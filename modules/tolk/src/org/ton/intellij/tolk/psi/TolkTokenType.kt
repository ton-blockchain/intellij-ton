package org.ton.intellij.tolk.psi

import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import org.ton.intellij.tolk.TolkLanguage
import org.ton.intellij.tolk.parser.TolkParserDefinition.Companion.BLOCK_COMMENT
import org.ton.intellij.tolk.parser.TolkParserDefinition.Companion.DOC_BLOCK_COMMENT
import org.ton.intellij.tolk.parser.TolkParserDefinition.Companion.EOL_COMMENT
import org.ton.intellij.tolk.parser.TolkParserDefinition.Companion.DOC_EOL_COMMENT
import org.ton.intellij.util.tokenSetOf

open class TolkTokenType(val name: String) : IElementType(name, TolkLanguage)

val TOLK_REGULAR_COMMENTS get() = tokenSetOf(BLOCK_COMMENT, EOL_COMMENT)
val TOLK_DOC_COMMENTS get() = tokenSetOf(DOC_BLOCK_COMMENT, DOC_EOL_COMMENT)
val TOLK_COMMENTS get() = TokenSet.orSet(TOLK_REGULAR_COMMENTS, TOLK_DOC_COMMENTS)

val TOLK_KEYWORDS = tokenSetOf(
    TolkElementTypes.RETURN_KEYWORD,
    TolkElementTypes.VAR_KEYWORD,
    TolkElementTypes.VAL_KEYWORD,
    TolkElementTypes.REPEAT_KEYWORD,
    TolkElementTypes.DO_KEYWORD,
    TolkElementTypes.WHILE_KEYWORD,
    TolkElementTypes.TRY_KEYWORD,
    TolkElementTypes.CATCH_KEYWORD,
    TolkElementTypes.IF_KEYWORD,
    TolkElementTypes.ELSE_KEYWORD,

    TolkElementTypes.TYPE_KEYWORD,

    TolkElementTypes.GLOBAL_KEYWORD,
    TolkElementTypes.ASM_KEYWORD,
    TolkElementTypes.OPERATOR_KEYWORD,
    TolkElementTypes.INFIX_KEYWORD,
    TolkElementTypes.CONST_KEYWORD,
    TolkElementTypes.BUILTIN_KEYWORD,
    TolkElementTypes.GET_KEYWORD,
    TolkElementTypes.LAZY_KEYWORD,
    TolkElementTypes.IMPORT_KEYWORD,
    TolkElementTypes.FUN_KEYWORD,
    TolkElementTypes.REDEF_KEYWORD,
    TolkElementTypes.AUTO_KEYWORD,
    TolkElementTypes.VOID_KEYWORD,
    TolkElementTypes.MUTATE_KEYWORD,
    TolkElementTypes.THROW_KEYWORD,
    TolkElementTypes.ASSERT_KEYWORD,
    TolkElementTypes.TOLK_KEYWORD,
    TolkElementTypes.BREAK_KEYWORD,
    TolkElementTypes.CONTINUE_KEYWORD,
    TolkElementTypes.NULL_KEYWORD,
    TolkElementTypes.ENUM_KEYWORD,
    TolkElementTypes.STRUCT_KEYWORD,
    TolkElementTypes.MATCH_KEYWORD,
    TolkElementTypes.AS_KEYWORD,
    TolkElementTypes.IS_KEYWORD,
    TolkElementTypes.NOT_IS_KEYWORD,
    TolkElementTypes.SELF_KEYWORD,
)
