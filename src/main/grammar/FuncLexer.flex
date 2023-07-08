package org.ton.intellij.func.lexer;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.containers.Stack;
import org.ton.intellij.func.psi.FuncElementTypes;

import static com.intellij.psi.TokenType.*;
import static org.ton.intellij.func.psi.FuncElementTypes.*;

%%

%{
  public _FuncLexer() {
    this((java.io.Reader)null);
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

    private IElementType commentStateToTokenType(int state) {
        switch (state) {
            case BLOCK_COMMENT:
                return FuncElementTypes.BLOCK_COMMENT;
            case DOC_COMMENT:
                return FuncElementTypes.DOC_COMMENT;
            default:
                throw new IllegalArgumentException("Unexpected state: " + state);
        }
    }
%}

%public
%function advance
%type IElementType
%eof{
  return;
%eof}
%xstate STRING RAW_STRING BLOCK_COMMENT DOC_COMMENT

DIGIT=[0-9]
DIGIT_OR_UNDERSCORE = [_0-9]
DIGITS = {DIGIT} {DIGIT_OR_UNDERSCORE}*
HEX_DIGIT=[0-9A-Fa-f]
HEX_DIGIT_OR_UNDERSCORE = [_0-9A-Fa-f]
WHITE_SPACE_CHAR=[\ \n\t\f]

IDENTIFIER_SYMBOLS=[\?\:\'\$\_]
IDENTIFIER_PART=[:digit:]|[:letter:]|IDENTIFIER_SYMBOLS

LINE_COMMENT=;;[^\n]*

INTEGER_LITERAL={DECIMAL_INTEGER_LITERAL}|{HEX_INTEGER_LITERAL}|{BIN_INTEGER_LITERAL}
DECIMAL_INTEGER_LITERAL=(0|([1-9]({DIGIT_OR_UNDERSCORE})*))
HEX_INTEGER_LITERAL=0[Xx]({HEX_DIGIT_OR_UNDERSCORE})*
BIN_INTEGER_LITERAL=0[Bb]({DIGIT_OR_UNDERSCORE})*

PLAIN_IDENTIFIER=([a-zA-Z_$?:'][0-9a-zA-Z_$?:']*)
ESCAPED_IDENTIFIER = `[^`\n]+`
IDENTIFIER = {PLAIN_IDENTIFIER}|{ESCAPED_IDENTIFIER}

// ANY_ESCAPE_SEQUENCE = \\[^]
THREE_QUO = (\"\"\")
QUOTE_SUFFIX = [s|a|u|h|H|c]
THREE_OR_MORE_QUO = ({THREE_QUO}\"*{QUOTE_SUFFIX}?)

ESCAPE_SEQUENCE=\\(u{HEX_DIGIT}{HEX_DIGIT}{HEX_DIGIT}{HEX_DIGIT}|[^\n])
CLOSING_QUOTE=\"{QUOTE_SUFFIX}?

REGULAR_STRING_PART=[^\\\"\n]+
SHORT_TEMPLATE_ENTRY=\${IDENTIFIER}
LONELY_BACKTICK=`

%%
{THREE_QUO}                      { pushState(RAW_STRING); return OPEN_QUOTE; }
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

\"                          { pushState(STRING); return OPEN_QUOTE; }
<STRING> \n                 { popState(); yypushback(1); return DANGLING_NEWLINE; }
<STRING> {CLOSING_QUOTE}    { popState(); return CLOSING_QUOTE; }
<STRING> {ESCAPE_SEQUENCE}  { return ESCAPE_SEQUENCE; }

<STRING, RAW_STRING> {REGULAR_STRING_PART}         { return FuncElementTypes.RAW_STRING_ELEMENT; }

"{--}" {
    return FuncElementTypes.BLOCK_COMMENT;
}

"{--" {
    pushState(DOC_COMMENT);
    commentDepth = 0;
    commentStart = getTokenStart();
}

"{-" {
    pushState(BLOCK_COMMENT);
    commentDepth = 0;
    commentStart = getTokenStart();
}

<BLOCK_COMMENT, DOC_COMMENT> {
    "{-" {
         commentDepth++;
    }

    <<EOF>> {
        int state = yystate();
        popState();
        zzStartRead = commentStart;
        return commentStateToTokenType(state);
    }

    "-}" {
        if (commentDepth > 0) {
            commentDepth--;
        }
        else {
             int state = yystate();
             popState();
             zzStartRead = commentStart;
             return commentStateToTokenType(state);
        }
    }

    [\s\S] {}
}

({WHITE_SPACE_CHAR})+ { return WHITE_SPACE; }

{LINE_COMMENT} { return LINE_COMMENT; }

{INTEGER_LITERAL} { return INTEGER_LITERAL; }

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

  "#include"               { return INCLUDE_MACRO; }
  "#pragma"                { return PRAGMA_MACRO; }

  "+"                      { return PLUS; }
  "-"                      { return MINUS; }
  "*"                      { return TIMES; }
  "/"                      { return DIV; }
  "%"                      { return MOD; }
  "?"                      { return QUEST; }
  ":"                      { return COLON; }
  "."                      { return DOT; }
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

{IDENTIFIER} { return IDENTIFIER; }

[\s\S]       { return BAD_CHARACTER; }

<STRING, RAW_STRING, BLOCK_COMMENT, DOC_COMMENT> .
             { return BAD_CHARACTER; }
