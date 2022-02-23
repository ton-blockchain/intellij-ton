package com.github.andreypfau.intellijton.tlb.parser;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.containers.Stack;

import static com.intellij.psi.TokenType.BAD_CHARACTER;
import static com.intellij.psi.TokenType.WHITE_SPACE;
import static com.github.andreypfau.intellijton.tlb.psi.TlbTypes.*;

%%
%unicode
%public
%class _TlbLexer
%implements FlexLexer

%{
    public _TlbLexer() {
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

EOL=\R

WHITE_SPACE_CHAR = [\ \n\t\f]
HEX_TAG=#([0-9a-f]+|_)\s
BINARY_TAG=\$([01]*|_)\s

LINE_DOCUMENTATION = "/""/""/"[^\n]*
LINE_COMMENT = "/""/"[^\n]*

NUMBER=[0-9]+
IDENTIFIER=[a-zA-Z_][0-9a-zA-Z0-9_]*

%xstate BLOCK_COMMENT_STATE, DOC_COMMENT_STATE

%%

"/**/" { return BLOCK_COMMENT; }
"/**" {
          pushState(DOC_COMMENT_STATE);
          commentDepth = 0;
          commentStart = getTokenStart();
}

"/*" {
          pushState(BLOCK_COMMENT_STATE);
          commentDepth = 0;
          commentStart = getTokenStart();
}

<BLOCK_COMMENT_STATE, DOC_COMMENT_STATE> {
    "/*" {
          commentDepth++;
      }

    <<EOF>> {
          int state = yystate();
          popState();
          zzStartRead = commentStart;
          return commentStateToTokenType(state);
      }

    "*/" {
          if (commentDepth > 0) {
              commentDepth--;
          } else {
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

{NUMBER} { return NUMBER; }
 
"Type" { return TYPE; }
"EMPTY" { return EMPTY; }
"Any" { return ANY; }
"Cell" { return CELL; }
"int " { return INT; }
"uint" { return UINT; }
"bits" { return BITS; }
"uint1" { return UINT1; }
"uint2" { return UINT2; }
"uint4" { return UINT4; }
"uint8" { return UINT8; }
"uint16" { return UINT16; }
"uint32" { return UINT32; }
"uint64" { return UINT64; }
"uint128" { return UINT128; }
"uint256" { return UINT256; }
"uint257" { return UINT257; }
"bits1" { return BITS1; }
"bits2" { return BITS2; }
"bits4" { return BITS4; }
"bits8" { return BITS8; }
"bits16" { return BITS16; }
"bits32" { return BITS32; }
"bits64" { return BITS64; }
"bits128" { return BITS128; }
"bits256" { return BITS256; }
"bits512" { return BITS512; }
"bits1023" { return BITS1023; }

{IDENTIFIER} { return IDENTIFIER; }

"+" { return PLUS; }
"-" { return MINUS; }
"*" { return TIMES; }
":" { return COLUMN; }
";" { return SEMICOLUMN; }
"(" { return LPAREN; }
")" { return RPAREN; }
"{" { return LBRACE; }
"}" { return RBRACE; }
"[" { return LBRACKET; }
"]" { return RBRACKET; }
"=" { return EQUALS; }
"?" { return QUESTION; }
"." { return DOT; }
"~" { return TILDE; }
"^" { return CIRCUMFLEX; }
"==" { return EQ; }
"<" { return LESS; }
">" { return GREATER; }
"<=" { return LEQ; }
">=" { return GEQ; }
"!=" { return NEQ; }

{HEX_TAG} { return HEX_TAG; }
{BINARY_TAG} { return BINARY_TAG; }

"##" { return DOUBLE_TAG; }
"#<" { return NAT_LESS; }
"#<=" { return NAT_LEQ; }
"#" { return TAG; }

[^] { return BAD_CHARACTER; }
