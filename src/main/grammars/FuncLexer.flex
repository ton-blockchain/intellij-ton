package com.github.andreypfau.intellijton.func.lexer;

import com.intellij.psi.tree.IElementType;

import static com.intellij.psi.TokenType.BAD_CHARACTER;
import static com.intellij.psi.TokenType.WHITE_SPACE;
import static com.github.andreypfau.intellijton.func.psi.FuncTypes.*;

%%

%{
  public _FuncLexer() {
    this((java.io.Reader)null);
  }
%}

%{
    private int blockCommentDepth;
    private int blockDocDepth;
    private int blockCommentStartPos;
    private int blockDocStartPos;
%}

%public
%class _FuncLexer
%implements FlexLexer
%function advance
%type IElementType
%unicode
%eof{ return;
%eof}

/* Documentation */
LINE_DOCUMENTATION_START      = {SEMICOLON}{SEMICOLON}{SEMICOLON}
LINE_DOCUMENTATION            = {LINE_DOCUMENTATION_START}[^\n]*
BLOCK_DOCUMENTATION_START     = {LEFT_BRACE}{DASH}{DASH}
BLOCK_DOCUMENTATION_END       = {DASH}{RIGHT_BRACE}

/* Comments */
LINE_COMMENT_START  = {SEMICOLON}{SEMICOLON}
LINE_COMMENT        = {LINE_COMMENT_START}[^\n]*
BLOCK_COMMENT_START = {LEFT_BRACE}{DASH}
BLOCK_COMMENT_END   = {DASH}{RIGHT_BRACE}

SEMICOLON           = \;
DASH                = \-
LEFT_BRACE          = \{
RIGHT_BRACE         = \}

EOL=\R
WHITE_SPACE=\s+
DECIMNAL_NUMBER_LITERAL=(-?[0-9][_0-9]*)
HEX_NUMBER_LITERAL=(0[xX][_0-9a-fA-F]+)
BINARY_NUMBER_LITERAL=(0[bB][_0-1]+)
STRING_LITERAL=(\"([^\"\r\n\\]|\\.)*\")|unicode(\"([^\"])*\")
IDENTIFIER=(`.*`)|(([a-zA-Z_]([a-zA-Z_0-9]|['?:])+)|([a-zA-Z]))

%state BLOCK_COMMENT_STATE, BLOCK_DOC_STATE

%%
<YYINITIAL> {
    "+"                             { return PLUS; }
    "-"                             { return MINUS; }
    "*"                             { return TIMES; }
    "/"                             { return DIVIDE; }
    "%"                             { return PERCENT; }
    "?"                             { return QUESTION; }
    ":"                             { return COLON; }
    ","                             { return COMMA; }
    "."                             { return DOT; }
    ";"                             { return SEMICOLON; }
    "("                             { return LPAREN; }
    ")"                             { return RPAREN; }
    "["                             { return LBRACKET; }
    "]"                             { return RBRACKET; }
    "{"                             { return LBRACE; }
    "}"                             { return RBRACE; }
    "="                             { return EQUALS; }
    "_"                             { return UNDERSCORE; }
    "<"                             { return LESS; }
    ">"                             { return GREATER; }
    "&"                             { return AND; }
    "|"                             { return OR; }
    "^"                             { return CIRCUMFLEX; }
    "~"                             { return TILDE; }
    "=="                            { return EQ; }
    "!="                            { return NEQ; }
    "<="                            { return LEQ; }
    ">="                            { return GEQ; }
    "<=>"                           { return SPACESHIP; }
    "<<"                            { return LSHIFT; }
    ">>"                            { return RSHIFT; }
    "~>>"                           { return RSHIFTR; }
    "^>>"                           { return RSHIFTC; }
    "~/"                            { return DIVR; }
    "^/"                            { return DIVC; }
    "~%"                            { return MODR; }
    "^%"                            { return MODC; }
    "/%"                            { return DIVMOD; }
    "+="                            { return PLUSLET; }
    "-="                            { return MINUSLET; }
    "*="                            { return TIMESLET; }
    "/="                            { return DIVLET; }
    "~/="                           { return DIVRLET; }
    "^/="                           { return DIVCLET; }
    "%="                            { return MODLET; }
    "~%="                           { return MODRLET; }
    "^%="                           { return MODCLET; }
    "<<="                           { return LSHIFTLET; }
    ">>="                           { return RSHIFTLET; }
    "~>>="                          { return RSHIFTRLET; }
    "^>>="                          { return RSHIFTCLET; }
    "&="                            { return ANDLET; }
    "|="                            { return ORLET; }
    "^="                            { return XORLET; }
    "return"                        { return RETURN; }
    "var"                           { return VAR; }
    "repeat"                        { return REPEAT; }
    "do"                            { return DO; }
    "while"                         { return WHILE; }
    "until"                         { return UNTIL; }
    "if"                            { return IF; }
    "ifnot"                         { return IFNOT; }
    "then"                          { return THEN; }
    "else"                          { return ELSE; }
    "elseif"                        { return ELSEIF; }
    "elseifnot"                     { return ELSEIFNOT; }
    "int"                           { return INT; }
    "cell"                          { return CELL; }
    "slice"                         { return SLICE; }
    "builder"                       { return BUILDER; }
    "cont"                          { return CONT; }
    "tuple"                         { return TUPLE; }
    "type"                          { return TYPE; }
    "true"                          { return TRUE; }
    "false"                         { return FALSE; }
    "->"                            { return MAPSTO; }
    "forall"                        { return FORALL; }
    "extern"                        { return EXTERN; }
    "global"                        { return GLOBAL; }
    "asm"                           { return ASM; }
    "impure"                        { return IMPURE; }
    "inline"                        { return INLINE; }
    "inline_ref"                    { return INLINE_REF; }
    "auto_apply"                    { return AUTO_APPLY; }
    "method_id"                     { return METHOD_ID; }
    "operator"                      { return OPERATOR; }
    "infixl"                        { return INFIXL; }
    "infixr"                        { return INFIXR; }

    {LINE_DOCUMENTATION}            { return LINE_DOCUMENTATION; }
    {LINE_COMMENT}                  { return LINE_COMMENT; }
    {BLOCK_DOCUMENTATION_START}     { blockDocDepth = 0; blockDocStartPos = getTokenStart(); yybegin(BLOCK_DOC_STATE); }
    {BLOCK_COMMENT_START}           { blockCommentDepth = 0; blockCommentStartPos = getTokenStart(); yybegin(BLOCK_COMMENT_STATE); }

    {DECIMNAL_NUMBER_LITERAL}       { return DECIMNAL_NUMBER_LITERAL; }
    {HEX_NUMBER_LITERAL}            { return HEX_NUMBER_LITERAL; }
    {BINARY_NUMBER_LITERAL}         { return BINARY_NUMBER_LITERAL; }
    {IDENTIFIER}                    { return IDENTIFIER; }
    {STRING_LITERAL}                { return STRING_LITERAL; }
    {WHITE_SPACE}                   { return WHITE_SPACE; }
}

<BLOCK_DOC_STATE> {
    {BLOCK_DOCUMENTATION_START} {
        blockDocDepth++;
    }
    <<EOF>> {
        yybegin(YYINITIAL);
        zzStartRead = blockDocStartPos;
        return BAD_CHARACTER;
    }
    {BLOCK_DOCUMENTATION_END} {
        if (blockDocDepth-- == -1) {
            yybegin(YYINITIAL);
            zzStartRead = blockDocStartPos;
            return BLOCK_DOCUMENTATION;
        }
    }
    [^] {}
}

<BLOCK_COMMENT_STATE> {
    {BLOCK_COMMENT_START} {
        blockCommentDepth++;
    }
    <<EOF>> {
        yybegin(YYINITIAL);
        zzStartRead = blockCommentStartPos;
        return BAD_CHARACTER;
    }
    {BLOCK_COMMENT_END} {
        if (blockCommentDepth-- == -1) {
            yybegin(YYINITIAL);
            zzStartRead = blockCommentStartPos;
            return BLOCK_COMMENT;
        }
    }
    [^] {}
}

[^] { return BAD_CHARACTER; }
