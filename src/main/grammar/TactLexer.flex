package org.ton.intellij.tact.parser;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;

import static com.intellij.psi.TokenType.BAD_CHARACTER;
import static com.intellij.psi.TokenType.WHITE_SPACE;
import static org.ton.intellij.tact.psi.TactElementTypes.*;

%%

%{
  public _TactLexer() {
    this((java.io.Reader)null);
  }
%}

%{
  /**
    * Dedicated storage for starting position of some previously successful
    * match
    */
  private int zzPostponedMarkedPos = -1;

  /**
    * Dedicated nested-comment level counter
    */
  private int zzNestedCommentLevel = 0;

  private int zzBlockDepth = 0;
  private int zzParenDepth = 0;
  private boolean zzStructScope = false;
  private boolean zzContractScope = false;
%}

%{
  IElementType imbueBlockComment() {
      assert(zzNestedCommentLevel == 0);
      yybegin(YYINITIAL);

      zzStartRead = zzPostponedMarkedPos;
      zzPostponedMarkedPos = -1;

//      if (yylength() >= 3) {
//          if (yycharat(2) == '!') {
//              return INNER_BLOCK_DOC_COMMENT;
//          } else if (yycharat(2) == '*' && (yylength() == 3 || yycharat(3) != '*' && yycharat(3) != '/')) {
//              return OUTER_BLOCK_DOC_COMMENT;
//          }
//      }

      return BLOCK_COMMENT;
  }
%}

%public
%class _TactLexer
%implements FlexLexer
%function advance
%type IElementType

%s IN_BLOCK_COMMENT
%s IN_NAME_ATTRIBUTE

%unicode

EOL=\R
WHITE_SPACE=\s+

WHITE_SPACE=[ \t\n\x0B\f\r]+
INTEGER_LITERAL=(0[xX][0-9a-fA-F][0-9a-fA-F_]*)|([0-9]+)
STRING_LITERAL=(\"([^\"\r\n\\]|\\.)*\")
IDENTIFIER=[a-zA-Z_][a-zA-Z0-9_]*
FUNC_IDENTIFIER=[a-zA-Z_][a-zA-Z0-9_?!:&']*

%%
<YYINITIAL> {
  {WHITE_SPACE}           { return WHITE_SPACE; }

  "/*"                    { yybegin(IN_BLOCK_COMMENT); yypushback(2); }
  "//".*                  { return LINE_COMMENT; }

  "{"                     { zzBlockDepth++; return LBRACE; }
  "}"                     {
          if (zzBlockDepth-- == 0) {
              zzStructScope = false;
              zzContractScope = false;
          }
          return RBRACE;
      }
  "["                     { return LBRACK; }
  "]"                     { return RBRACK; }
  "("                     { zzParenDepth++; return LPAREN; }
  ")"                     { zzParenDepth--; return RPAREN; }
  ":"                     { return COLON; }
  ";"                     { return SEMICOLON; }
  ","                     { return COMMA; }
  "."                     { return DOT; }
  "+"                     { return PLUS; }
  "-"                     { return MINUS; }
  "*"                     { return MUL; }
  "/"                     { return DIV; }
  "%"                     { return REM; }
  "&"                     { return AND; }
  "|"                     { return OR; }
  "^"                     { return XOR; }
  "<"                     { return LT; }
  ">"                     { return GT; }
  "="                     { return EQ; }
  "?"                     { return Q; }
  "!"                     { return EXCL; }
  "=="                    { return EQEQ; }
  "!="                    { return EXCLEQ; }
  ">="                    { return GTEQ; }
  "<="                    { return LTEQ; }
  ">>"                    { return GTGT; }
  "<<"                    { return LTLT; }
  "||"                    { return OROR; }
  "&&"                    { return ANDAND; }
  "!!"                    { return EXCLEXCL; }
  "if"                    { return IF_KEYWORD; }
  "else"                  { return ELSE_KEYWORD; }
  "while"                 { return WHILE_KEYWORD; }
  "do"                    { return DO_KEYWORD; }
  "until"                 { return UNTIL_KEYWORD; }
  "repeat"                { return REPEAT_KEYWORD; }
  "return"                { return RETURN_KEYWORD; }
  "extends"               { return EXTENDS_KEYWORD; }
  "mutates"               { return MUTATES_KEYWORD; }
  "virtual"               { return VIRTUAL_KEYWORD; }
  "override"              { return OVERRIDE_KEYWORD; }
  "inline"                { return INLINE_KEYWORD; }
  "native"                { return NATIVE_KEYWORD; }
  "let"                   { return LET_KEYWORD; }
  "const"                 { return CONST_KEYWORD; }
  "fun"                   { return FUN_KEYWORD; }
  "initOf"                { return INIT_OF_KEYWORD; }
  "as"                    { return AS_KEYWORD; }
  "abstract"              { return ABSTRACT_KEYWORD; }
  "import"                { return IMPORT_KEYWORD; }
  "struct"                { zzStructScope = true; return STRUCT_KEYWORD; }
  "message"               { return MESSAGE_KEYWORD; }
  "contract"              { zzContractScope = true; return CONTRACT_KEYWORD; }
  "trait"                 { return TRAIT_KEYWORD; }
  "with"                  { return WITH_KEYWORD; }
  "receive"               { return RECEIVE_KEYWORD; }
  "external"              { return EXTERNAL_KEYWORD; }
  "true"                  { return BOOLEAN_LITERAL; }
  "false"                 { return BOOLEAN_LITERAL; }
  "null"                  { return NULL_LITERAL; }
  "primitive"             { return PRIMITIVE_KEYWORD; }
  "self"                  { return SELF_KEYWORD; }
  "map"                   { return MAP_KEYWORD; }
  "bounced"               { return zzBlockDepth == 1 && zzContractScope ? BOUNCED_KEYWORD : IDENTIFIER; }
  "init"                  { return zzBlockDepth == 1 && zzParenDepth == 0 ? INIT_KEYWORD : IDENTIFIER; }
  "get"                   { return zzBlockDepth <= 1 ? GET_KEYWORD : IDENTIFIER; }
  "@interface"            { return INTERFACE_MACRO; }
  "@name"                 { yybegin(IN_NAME_ATTRIBUTE); yypushback(5); }

  {INTEGER_LITERAL}       { return INTEGER_LITERAL; }
  {STRING_LITERAL}        { return STRING_LITERAL; }
  {IDENTIFIER}            { return IDENTIFIER; }
}

<IN_BLOCK_COMMENT> {
  "/*"    { if (zzNestedCommentLevel++ == 0)
              zzPostponedMarkedPos = zzStartRead;
          }

  "*/"    { if (--zzNestedCommentLevel == 0)
              return imbueBlockComment();
          }

  <<EOF>> { zzNestedCommentLevel = 0; return imbueBlockComment(); }

  [^]     { }
}

<IN_NAME_ATTRIBUTE> {
   "@name" { return NAME_MACRO; }
   "("    { zzParenDepth++; return LPAREN; }
   ")"    { zzParenDepth--; yybegin(YYINITIAL); return RPAREN; }
   {FUNC_IDENTIFIER} { yybegin(YYINITIAL); return FUNC_IDENTIFIER; }
   [^]    { yybegin(YYINITIAL); yypushback(1); }
}

[^] { return BAD_CHARACTER; }
