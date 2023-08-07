package org.ton.intellij.tact.psi

import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import org.ton.intellij.func.FuncLanguage
import org.ton.intellij.tact.psi.TactElementTypes.*
import org.ton.intellij.util.tokenSetOf

open class TactTokenType(debugName: String) : IElementType(debugName, FuncLanguage)

val TACT_REGULAR_COMMENTS = tokenSetOf(BLOCK_COMMENT, LINE_COMMENT)
val TACT_DOC_COMMENTS = tokenSetOf()
val TACT_COMMENTS = TokenSet.orSet(TACT_REGULAR_COMMENTS, TACT_DOC_COMMENTS)

val TACT_STRING_LITERALS = TokenSet.create(STRING_LITERAL)
val TACT_MACROS = tokenSetOf(NAME_MACRO, INTERFACE_MACRO)

val TACT_KEYWORDS = tokenSetOf(
    IF_KEYWORD,
    ELSE_KEYWORD,
    WHILE_KEYWORD,
    DO_KEYWORD,
    UNTIL_KEYWORD,
    REPEAT_KEYWORD,
    RETURN_KEYWORD,
    EXTENDS_KEYWORD,
    MUTATES_KEYWORD,
    VIRTUAL_KEYWORD,
    OVERRIDE_KEYWORD,
    INLINE_KEYWORD,
    NATIVE_KEYWORD,
    LET_KEYWORD,
    CONST_KEYWORD,
    FUN_KEYWORD,
    INIT_OF_KEYWORD,
    GET_KEYWORD,
    AS_KEYWORD,
    ABSTRACT_KEYWORD,
    IMPORT_KEYWORD,
    STRUCT_KEYWORD,
    MESSAGE_KEYWORD,
    CONTRACT_KEYWORD,
    TRAIT_KEYWORD,
    WITH_KEYWORD,
    INIT_KEYWORD,
    RECEIVE_KEYWORD,
    BOUNCED_KEYWORD,
    EXTERNAL_KEYWORD,
    INT_OF_KEYWORD
)
