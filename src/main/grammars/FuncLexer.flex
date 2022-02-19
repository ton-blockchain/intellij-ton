package com.github.andreypfau.intellijton.func.lexer;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.containers.Stack;

import static com.intellij.psi.TokenType.*;
import static com.github.andreypfau.intellijton.func.psi.FuncTypes.*;

%%

%unicode
%public
%class _FuncLexer
%implements FlexLexer

%{
  public _FuncLexer() {
    this((java.io.Reader)null);
  }
%}

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
    private final Stack<State> states = new Stack<>();
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
            case BLOCK_COMMENT_STATE:
                return BLOCK_COMMENT;
            case DOC_COMMENT_STATE:
                return BLOCK_DOCUMENTATION;
            default:
                throw new IllegalArgumentException("Unexpected state: " + state);
        }
    }
%}

%function advance
%type IElementType
%eof{
  return;
%eof}

DIGIT = [0-9]
BIN_DIGIT = [01]
HEX_DIGIT = [0-9A-Fa-f]
WHITE_SPACE_CHAR = [\ \n\t\f]

LETTER = [a-zA-Z]|_
IDENTIFIER_PART = {DIGIT}|{LETTER}|"?"|":"":"
PLAIN_IDENTIFIER = {LETTER} {IDENTIFIER_PART}*
ESCAPED_IDENTIFIER = `[^`\n]+`
IDENTIFIER = {PLAIN_IDENTIFIER}|{ESCAPED_IDENTIFIER}

LINE_DOCUMENTATION = ";"";"";"[^\n]*
LINE_COMMENT = ";"";"[^\n]*

DECIMAL_INTEGER_LITERAL=(0|([1-9]({DIGIT})*))
HEX_INTEGER_LITERAL=0[Xx]({HEX_DIGIT})*
BIN_INTEGER_LITERAL=0[Bb]({BIN_DIGIT})*
INTEGER_LITERAL={DECIMAL_INTEGER_LITERAL}|{HEX_INTEGER_LITERAL}|{BIN_INTEGER_LITERAL}

%xstate BLOCK_COMMENT_STATE, DOC_COMMENT_STATE

%%

"{--}" {
    return BLOCK_COMMENT;
}

"{--" {
    pushState(DOC_COMMENT_STATE);
    commentDepth = 0;
    commentStart = getTokenStart();
}

"{-" {
    pushState(BLOCK_COMMENT_STATE);
    commentDepth = 0;
    commentStart = getTokenStart();
}

<BLOCK_COMMENT_STATE, DOC_COMMENT_STATE> {
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
{LINE_DOCUMENTATION} { return LINE_DOCUMENTATION; }
{LINE_COMMENT} { return LINE_COMMENT; }

{INTEGER_LITERAL} { return INTEGER_LITERAL; }

"return" { return RETURN; }
"var" { return VAR; }
"repeat" { return REPEAT; }
"do" { return DO; }
"while" { return WHILE; }
"until" { return UNTIL; }
"if" { return IF; }
"ifnot" { return IFNOT; }
"then" { return THEN; }
"else" { return ELSE; }
"elseif" { return ELSEIF; }
"elseifnot" { return ELSEIFNOT; }
"int" { return INT; }
"cell" { return CELL; }
"slice" { return SLICE; }
"builder" { return BUILDER; }
"cont" { return CONT; }
"tuple" { return TUPLE; }
"type" { return TYPE; }
"true" { return TRUE; }
"false" { return FALSE; }
"forall" { return FORALL; }
"extern" { return EXTERN; }
"global" { return GLOBAL; }
"asm" { return ASM; }
"impure" { return IMPURE; }
"inline" { return INLINE; }
"inline_ref" { return INLINE_REF; }
"auto_apply" { return AUTO_APPLY; }
"method_id" { return METHOD_ID; }
"operator" { return OPERATOR; }
"infixl" { return INFIXL; }
"infixr" { return INFIXR; }

{IDENTIFIER} { return IDENTIFIER; }

"+" { return PLUS; }
"-" { return MINUS; }
"*" { return TIMES; }
"/" { return DIVIDE; }
"%" { return PERCENT; }
"?" { return QUESTION; }
":" { return COLON; }
"," { return COMMA; }
"." { return DOT; }
";" { return SEMICOLON; }
"(" { return LPAREN; }
")" { return RPAREN; }
"[" { return LBRACKET; }
"]" { return RBRACKET; }
"{" { return LBRACE; }
"}" { return RBRACE; }
"=" { return EQUALS; }
"<" { return LESS; }
">" { return GREATER; }
"&" { return AND; }
"|" { return OR; }
"^" { return CIRCUMFLEX; }
"~" { return TILDE; }

"==" { return EQ; }
"!=" { return NEQ; }
"<=" { return LEQ; }
">=" { return GEQ; }
"->" { return MAPSTO; }
"<=>" { return SPACESHIP; }
"<<" { return LSHIFT; }
">>" { return RSHIFT; }
"~>>" { return RSHIFTR; }
"^>>" { return RSHIFTC; }
"~/" { return DIVR; }
"^/" { return DIVC; }
"~%" { return MODR; }
"^%" { return MODC; }
"/%" { return DIVMOD; }
"+=" { return PLUSLET; }
"-=" { return MINUSLET; }
"*=" { return TIMESLET; }
"/=" { return DIVLET; }
"~/=" { return DIVRLET; }
"^/=" { return DIVCLET; }
"%=" { return MODLET; }
"~%=" { return MODRLET; }
"^%=" { return MODCLET; }
"<<=" { return LSHIFTLET; }
">>=" { return RSHIFTLET; }
"~>>=" { return RSHIFTRLET; }
"^>>=" { return RSHIFTCLET; }
"&=" { return ANDLET; }
"|=" { return ORLET; }
"^=" { return XORLET; }

//{IDENTIFIER} { return IDENTIFIER; }

[\s\S] { return BAD_CHARACTER; }
<BLOCK_COMMENT_STATE, DOC_COMMENT_STATE> . { return BAD_CHARACTER; }
