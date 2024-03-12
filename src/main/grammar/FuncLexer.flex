package org.ton.intellij.func.lexer;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.containers.Stack;
import org.ton.intellij.func.parser.FuncParserDefinition;
import org.ton.intellij.func.psi.FuncElementTypes;

import static com.intellij.psi.TokenType.*;
import static org.ton.intellij.func.psi.FuncElementTypes.*;
import static org.ton.intellij.func.parser.FuncParserDefinition.*;

%%

%{
  public _FuncLexer() {
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
  IElementType imbueBlockComment() {
      assert(zzNestedCommentLevel == 0);
      yybegin(YYINITIAL);

      zzStartRead = zzPostponedMarkedPos;
      zzPostponedMarkedPos = -1;

      if (yylength() >= 3) {
          if (yycharat(2) == '-' && (yylength() == 3 || yycharat(3) != '-' && yycharat(3) != '}')) {
              return BLOCK_DOC_COMMENT;
          }
      }

      return BLOCK_COMMENT;
  }

  IElementType imbueOuterEolComment(){
      yybegin(YYINITIAL);

      zzStartRead = zzPostponedMarkedPos;
      zzPostponedMarkedPos = -1;

      return EOL_DOC_COMMENT;
  }
%}

%unicode
%class _FuncLexer
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
%xstate STRING RAW_STRING DOC_COMMENT

%s IN_BLOCK_COMMENT
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

LINE_DOC_COMMENT=;;;[^\n]*
LINE_COMMENT=;;[^\n]*

INTEGER_LITERAL={DECIMAL_INTEGER_LITERAL}|{HEX_INTEGER_LITERAL}|{BIN_INTEGER_LITERAL}
DECIMAL_INTEGER_LITERAL=-?(0|([1-9]({DIGIT_OR_UNDERSCORE})*))
HEX_INTEGER_LITERAL=0[Xx]({HEX_DIGIT_OR_UNDERSCORE})*
BIN_INTEGER_LITERAL=0[Bb]({DIGIT_OR_UNDERSCORE})*

//PLAIN_IDENTIFIER=([a-zA-Z_$?:'~][0-9a-zA-Z_$?:'+\-\=\^><&|/%]*)
//PLAIN_IDENTIFIER=[.~]?[^\s;,\(\)\[\]\{\},.~\"\+\-\*\/%]+
//PLAIN_IDENTIFIER=^(?!{\-|;;)[.~]?[^\s;,.~\(\)\[\]\"\{\}]+
PLAIN_IDENTIFIER=[^\s()\[\],.;~\"\{\}]+
QUOTE_ESCAPED_IDENTIFIER = (`[^`\n]+`)|(_[^`\n]+_)
IDENTIFIER = [.~]?({QUOTE_ESCAPED_IDENTIFIER}|{PLAIN_IDENTIFIER})

// ANY_ESCAPE_SEQUENCE = \\[^]
THREE_QUO = (\"\"\")
QUOTE_SUFFIX = [s|a|u|h|H|c]
THREE_OR_MORE_QUO = ({THREE_QUO}\"*{QUOTE_SUFFIX}?)

ESCAPE_SEQUENCE=\\(u{HEX_DIGIT}{HEX_DIGIT}{HEX_DIGIT}{HEX_DIGIT}|[^\n])
CLOSING_QUOTE=\"{QUOTE_SUFFIX}?

REGULAR_STRING_PART=[^\\\"\n]+

// !(!a|b) is a (set) difference between a and b.
EOL_DOC_LINE  = {LINE_WS}*!(!(";;;".*)|(";;;;".*))

%%
<YYINITIAL> {
      {WHITE_SPACE}            { return WHITE_SPACE; }

      \"                       { pushState(STRING); return OPEN_QUOTE; }

      "{-"                     { yybegin(IN_BLOCK_COMMENT); yypushback(2); }
      ";;;;" .*                { return EOL_COMMENT; }
      {EOL_DOC_LINE}           { yybegin(IN_EOL_DOC_COMMENT);
                                 zzPostponedMarkedPos = zzStartRead; }
      ";;" .*                  { return EOL_COMMENT; }

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
      "_"                      { return UNDERSCORE; }
      "<"                      { return LT; }
      ">"                      { return GT; }
      "&"                      { return AND; }
      "|"                      { return OR; }
      "^"                      { return XOR; }
      "~"                      { return TILDE; }

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

      "return"                 { return RETURN_KEYWORD; }
      "var"                    { return VAR_KEYWORD; }
      "repeat"                 { return REPEAT_KEYWORD; }
      "do"                     { return DO_KEYWORD; }
      "while"                  { return WHILE_KEYWORD; }
      "until"                  { return UNTIL_KEYWORD; }
      "try"                    { return TRY_KEYWORD; }
      "catch"                  { return CATCH_KEYWORD; }
      "if"                     { return IF_KEYWORD; }
      "ifnot"                  { return IFNOT_KEYWORD; }
      "then"                   { return THEN_KEYWORD; }
      "else"                   { return ELSE_KEYWORD; }
      "elseif"                 { return ELSEIF_KEYWORD; }
      "elseifnot"              { return ELSEIFNOT_KEYWORD; }
      "int"                    { return INT_KEYWORD; }
      "cell"                   { return CELL_KEYWORD; }
      "slice"                  { return SLICE_KEYWORD; }
      "builder"                { return BUILDER_KEYWORD; }
      "cont"                   { return CONT_KEYWORD; }
      "tuple"                  { return TUPLE_KEYWORD; }
      "type"                   { return TYPE_KEYWORD; }
      "forall"                 { return FORALL_KEYWORD; }
      "extern"                 { return EXTERN_KEYWORD; }
      "global"                 { return GLOBAL_KEYWORD; }
      "asm"                    { return ASM_KEYWORD; }
      "impure"                 { return IMPURE_KEYWORD; }
      "inline"                 { return INLINE_KEYWORD; }
      "inline_ref"             { return INLINE_REF_KEYWORD; }
      "auto_apply"             { return AUTO_APPLY_KEYWORD; }
      "method_id"              { return METHOD_ID_KEYWORD; }
      "operator"               { return OPERATOR_KEYWORD; }
      "infix"                  { return INFIX_KEYWORD; }
      "infixl"                 { return INFIXL_KEYWORD; }
      "infixr"                 { return INFIXR_KEYWORD; }
      "const"                  { return CONST_KEYWORD; }
      "true"                   { return TRUE_KEYWORD; }
      "false"                  { return FALSE_KEYWORD; }
      "nil"                    { return NULL_KEYWORD; }
      "Nil"                    { return NIL_KEYWORD; }

      "#include"               { return INCLUDE_MACRO; }
      "#pragma"                { return PRAGMA_MACRO; }

      {INTEGER_LITERAL}        { return INTEGER_LITERAL; }
      {THREE_QUO}              { pushState(RAW_STRING); return OPEN_QUOTE; }
      {IDENTIFIER}             { return IDENTIFIER; }
}

<RAW_STRING> \n                  { return FuncElementTypes.RAW_STRING_ELEMENT; }
<RAW_STRING> \"                  { return FuncElementTypes.RAW_STRING_ELEMENT; }
<RAW_STRING> \\                  { return FuncElementTypes.RAW_STRING_ELEMENT; }
<RAW_STRING> {THREE_OR_MORE_QUO} {
                                    int length = yytext().length();
                                    if (length <= 3) { // closing """
                                        popState();
                                        return FuncElementTypes.CLOSING_QUOTE;
                                    }
                                    else { // some quotes at the end of a string, e.g. """ "foo""""
                                        yypushback(3); // return the closing quotes (""") to the stream
                                        return FuncElementTypes.RAW_STRING_ELEMENT;
                                    }
                                 }

<STRING> \n                 { popState(); yypushback(1); return DANGLING_NEWLINE; }
<STRING> {CLOSING_QUOTE}    { popState(); return CLOSING_QUOTE; }
<STRING> {ESCAPE_SEQUENCE}  { return ESCAPE_SEQUENCE; }

<STRING, RAW_STRING> {REGULAR_STRING_PART}         { return FuncElementTypes.RAW_STRING_ELEMENT; }

<IN_BLOCK_COMMENT> {
  "{-"  { if (zzNestedCommentLevel++ == 0)
              zzPostponedMarkedPos = zzStartRead;
          }

  "-}"    { if (--zzNestedCommentLevel == 0)
              return imbueBlockComment();
          }

  <<EOF>> { zzNestedCommentLevel = 0; return imbueBlockComment(); }

  [^]     { }
}

<IN_EOL_DOC_COMMENT> {
  {EOL_WS}{LINE_WS}*";;;;"   { yybegin(YYINITIAL);
                               yypushback(yylength());
                               return imbueOuterEolComment();}
  {EOL_WS}{EOL_DOC_LINE}     {}
  <<EOF>>                    { return imbueOuterEolComment(); }
  [^]                        { yybegin(YYINITIAL);
                               yypushback(1);
                               return imbueOuterEolComment();}
}

[^] { return BAD_CHARACTER; }
