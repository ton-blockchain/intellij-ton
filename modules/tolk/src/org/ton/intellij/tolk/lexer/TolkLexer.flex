package org.ton.intellij.tolk.lexer;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.containers.Stack;
import org.ton.intellij.tolk.psi.TolkElementTypes;

import static com.intellij.psi.TokenType.BAD_CHARACTER;
import static com.intellij.psi.TokenType.WHITE_SPACE;
import static org.ton.intellij.tolk.parser.TolkParserDefinition.*;
import static org.ton.intellij.tolk.psi.TolkElementTypes.*;

%%

%{
  public _TolkLexer() {
    this((java.io.Reader)null);
 }
%}

%{
  /**
    * '#+' stride demarking start/end of raw string/byte literal
    */
  private int zzShaStride = -1;

  /**
    * Dedicated storage for starting position of some previously successful
    * match
    */
  private int zzPostponedMarkedPos = -1;

  /**
    * Dedicated nested-comment level counter
    */
  private int zzNestedCommentLevel = 0;
%}

%{
  IElementType commentStateToTokenType(int state) {
      switch (state) {
          case IN_BLOCK_COMMENT:
              return BLOCK_COMMENT;
          case IN_BLOCK_DOC:
              return DOC_COMMENT;
          default:
              throw new IllegalStateException("Unexpected state: " + state);
      }
  }

  IElementType imbueOuterEolComment(){
      yybegin(YYINITIAL);

      zzStartRead = zzPostponedMarkedPos;
      zzPostponedMarkedPos = -1;

      return DOC_COMMENT;
  }
%}

%unicode
%class _TolkLexer
%implements FlexLexer

%{
      private static final class State {
          final int lBraceCount;
          final int state;

          public State(int state, int lBraceCount) {
              this.state = state;
              this.lBraceCount = lBraceCount;
          }

          @Override
          public String toString() {
              return "yystate = " + state + (lBraceCount == 0 ? "" : "lBraceCount = " + lBraceCount);
          }
      }

      private final Stack<State> states = new Stack<State>();
      private int lBraceCount;

      private int commentStart;
      private int commentDepth;

      private void pushState(int state) {
          states.push(new State(yystate(), lBraceCount));
          lBraceCount = 0;
          yybegin(state);
      }

      private void popState() {
          State state = states.pop();
          lBraceCount = state.lBraceCount;
          yybegin(state.state);
      }
%}

%public
%function advance
%type IElementType
%eof{
  return;
%eof}
%xstate STRING RAW_STRING

%s IN_BLOCK_COMMENT IN_BLOCK_DOC
%s IN_EOL_DOC_COMMENT


///////////////////////////////////////////////////////////////////////////////////////////////////
// Whitespaces
///////////////////////////////////////////////////////////////////////////////////////////////////

EOL_WS           = \n | \r | \r\n
LINE_WS          = [\ \t]
WHITE_SPACE_CHAR = {EOL_WS} | {LINE_WS}
WHITE_SPACE      = {WHITE_SPACE_CHAR}+


DIGIT=[0-9]
DIGIT_OR_UNDERSCORE = [_0-9]
DIGITS = {DIGIT} {DIGIT_OR_UNDERSCORE}*
HEX_DIGIT=[0-9A-Fa-f]
HEX_DIGIT_OR_UNDERSCORE = [_0-9A-Fa-f]
WHITE_SPACE_CHAR=[\ \n\t\f]

IDENTIFIER_SYMBOLS=[\?\:\'\$\_]
IDENTIFIER_PART=[:digit:]|[:letter:]|IDENTIFIER_SYMBOLS

INTEGER_LITERAL=({DECIMAL_INTEGER_LITERAL}|{HEX_INTEGER_LITERAL}|{BIN_INTEGER_LITERAL})
DECIMAL_INTEGER_LITERAL=(0|([1-9]({DIGIT_OR_UNDERSCORE})*))
HEX_INTEGER_LITERAL=0[Xx]({HEX_DIGIT_OR_UNDERSCORE})*
BIN_INTEGER_LITERAL=0[Bb](0|1|_)*

PLAIN_IDENTIFIER=[a-zA-Z$_][a-zA-Z0-9$_]*
QUOTE_ESCAPED_IDENTIFIER = (`[^`\n]+`)|(_[^_\n\w,]+_)
IDENTIFIER = {QUOTE_ESCAPED_IDENTIFIER}|{PLAIN_IDENTIFIER}
//VERSION_VALUE = (=|>|>=|<|<=|\^)?\d+(\.\d+)?(\.\d+)?

// ANY_ESCAPE_SEQUENCE = \\[^]
THREE_QUO = (\"\"\")
QUOTE_SUFFIX = [s|a|u|h|H|c]
THREE_OR_MORE_QUO = ({THREE_QUO}\"*{QUOTE_SUFFIX}?)

ESCAPE_SEQUENCE=\\(u{HEX_DIGIT}{HEX_DIGIT}{HEX_DIGIT}{HEX_DIGIT}|[^\n])
CLOSING_QUOTE=\"{QUOTE_SUFFIX}?

REGULAR_STRING_PART=[^\\\"\n]+

// !(!a|b) is a (set) difference between a and b.
EOL_DOC_LINE  = {LINE_WS}*!(!(("///").*)|(("////").*))

// TODO: tolk, val, bool, enum, struct, export
//

%%
<YYINITIAL> {
      {WHITE_SPACE}            { return WHITE_SPACE; }

      \"                       { pushState(STRING); return OPEN_QUOTE; }

      "/**/" {
          return BLOCK_COMMENT;
      }
      "/**" {
          pushState(IN_BLOCK_DOC);
          commentDepth = 0;
          commentStart = getTokenStart();
      }
      "/*" {
          pushState(IN_BLOCK_COMMENT);
          commentDepth = 0;
          commentStart = getTokenStart();
      }
      ("////") .*                { return EOL_COMMENT; }
      {EOL_DOC_LINE}           { yybegin(IN_EOL_DOC_COMMENT);
                                 zzPostponedMarkedPos = zzStartRead; }
      ("//") .*                  { return EOL_COMMENT; }

      "+"                      { return PLUS; }
      "-"                      { return MINUS; }
      "*"                      { return TIMES; }
      "/"                      { return DIV; }
      "%"                      { return MOD; }
      "?"                      { return QUEST; }
      ":"                      { return COLON; }
      ","                      { return COMMA; }
      ";"                      { return SEMICOLON; }
      "{"                      { return LBRACE; }
      "}"                      { return RBRACE; }
      "["                      { return LBRACK; }
      "]"                      { return RBRACK; }
      "("                      { return LPAREN; }
      ")"                      { return RPAREN; }
      "="                      { return EQ; }
      "<"                      { return LT; }
      ">"                      { return GT; }
      "&&"                     { return ANDAND; }
      "&"                      { return AND; }
      "||"                     { return OROR; }
      "|"                      { return OR; }
      "^"                      { return XOR; }
      "#"                      { return SHA; }
      "!"                      { return EXCL; }
      "."                      { return DOT; }
      "@"                      { return AT; }

      "?."                     { return SAFE_ACCESS; }
      "=="                     { return EQEQ; }
      "!="                     { return NEQ; }
      "<="                     { return LEQ; }
      ">="                     { return GEQ; }
      "<=>"                    { return SPACESHIP; }
      "<<"                     { return LSHIFT; }
      ">>"                     { return RSHIFT; }
      "~>>"                    { return RSHIFTR; }
      "^>>"                    { return RSHIFTC; }
      "~/"                     { return DIVR; }
      "^/"                     { return DIVC; }
      "~%"                     { return MODR; }
      "^%"                     { return MODC; }
      "/%"                     { return DIVMOD; }
      "+="                     { return PLUSLET; }
      "-="                     { return MINUSLET; }
      "*="                     { return TIMESLET; }
      "/="                     { return DIVLET; }
      "~/="                    { return DIVRLET; }
      "^/="                    { return DIVCLET; }
      "%="                     { return MODLET; }
      "~%="                    { return MODRLET; }
      "^%="                    { return MODCLET; }
      "<<="                    { return LSHIFTLET; }
      ">>="                    { return RSHIFTLET; }
      "~>>="                   { return RSHIFTRLET; }
      "^>>="                   { return RSHIFTCLET; }
      "&="                     { return ANDLET; }
      "|="                     { return ORLET; }
      "^="                     { return XORLET; }
      "->"                     { return MAPSTO; }
      "~"                      { return TILDE; }

      "return"                 { return RETURN_KEYWORD; }
      "var"                    { return VAR_KEYWORD; }
      "repeat"                 { return REPEAT_KEYWORD; }
      "do"                     { return DO_KEYWORD; }
      "while"                  { return WHILE_KEYWORD; }
      "try"                    { return TRY_KEYWORD; }
      "catch"                  { return CATCH_KEYWORD; }
      "if"                     { return IF_KEYWORD; }
      "else"                   { return ELSE_KEYWORD; }
      "global"                 { return GLOBAL_KEYWORD; }
      "asm"                    { return ASM_KEYWORD; }
      "operator"               { return OPERATOR_KEYWORD; }
      "infix"                  { return INFIX_KEYWORD; }
      "const"                  { return CONST_KEYWORD; }
      "true"                   { return TRUE_KEYWORD; }
      "false"                  { return FALSE_KEYWORD; }
      "null"                   { return NULL_KEYWORD; }
      "builtin"                { return BUILTIN_KEYWORD; }
      "import"                 { return IMPORT_KEYWORD; }
      "fun"                    { return FUN_KEYWORD; }
      "redef"                  { return REDEF_KEYWORD; }
      "auto"                   { return AUTO_KEYWORD; }
      "mutate"                 { return MUTATE_KEYWORD; }
      "assert"                 { return ASSERT_KEYWORD; }
      "throw"                  { return THROW_KEYWORD; }
      "void"                   { return VOID_KEYWORD; }
      "tolk"                   { return TOLK_KEYWORD; }
      "type"                   { return TYPE_KEYWORD; }
      "val"                    { return VAL_KEYWORD; }
      "enum"                   { return ENUM_KEYWORD; }
      "struct"                 { return STRUCT_KEYWORD; }
      "export"                 { return EXPORT_KEYWORD; }
      "break"                  { return BREAK_KEYWORD; }
      "continue"               { return CONTINUE_KEYWORD; }
      "as"                     { return AS_KEYWORD; }

      {INTEGER_LITERAL}        { return INTEGER_LITERAL; }
      {THREE_QUO}              { pushState(RAW_STRING); return OPEN_QUOTE; }
      {IDENTIFIER}             { return IDENTIFIER; }
}

<RAW_STRING> \n                  { return TolkElementTypes.RAW_STRING_ELEMENT; }
<RAW_STRING> \"                  { return TolkElementTypes.RAW_STRING_ELEMENT; }
<RAW_STRING> \\                  { return TolkElementTypes.RAW_STRING_ELEMENT; }
<RAW_STRING> {THREE_OR_MORE_QUO} {
                                    int length = yytext().length();
                                    if (length <= 3) { // closing """
                                        popState();
                                        return TolkElementTypes.CLOSING_QUOTE;
                                    }
                                    else { // some quotes at the end of a string, e.g. """ "foo""""
                                        yypushback(3); // return the closing quotes (""") to the stream
                                        return TolkElementTypes.RAW_STRING_ELEMENT;
                                    }
                                 }

<STRING> \n                 { popState(); yypushback(1); return DANGLING_NEWLINE; }
<STRING> {CLOSING_QUOTE}    { popState(); return CLOSING_QUOTE; }
<STRING> {ESCAPE_SEQUENCE}  { return ESCAPE_SEQUENCE; }

<STRING, RAW_STRING> {REGULAR_STRING_PART}         { return TolkElementTypes.RAW_STRING_ELEMENT; }

<IN_BLOCK_COMMENT, IN_BLOCK_DOC> {
    "/*" {
         commentDepth = 1;
    }

    <<EOF>> {
          int state = yystate();
          popState();
          zzStartRead = commentStart;
          return commentStateToTokenType(state);
    }

    "*/" {
//        if (commentDepth > 0) {
//            commentDepth--;
//        }
//        else {
             int state = yystate();
             popState();
             zzStartRead = commentStart;
             return commentStateToTokenType(state);
//        }
    }

    [\s\S] {}
}

<IN_EOL_DOC_COMMENT> {
  {EOL_WS}{LINE_WS}*("////")   { yybegin(YYINITIAL);
                               yypushback(yylength());
                               return imbueOuterEolComment();}
  {EOL_WS}{EOL_DOC_LINE}     {}
  <<EOF>>                    { return imbueOuterEolComment(); }
  [^]                        { yybegin(YYINITIAL);
                               yypushback(1);
                               return imbueOuterEolComment();}
}

[^] { return BAD_CHARACTER; }
