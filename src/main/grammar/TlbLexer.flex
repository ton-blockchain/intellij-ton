package org.ton.intellij.tlb.lexer;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.containers.Stack;

import static com.intellij.psi.TokenType.*;
import static org.ton.intellij.tlb.psi.TlbElementTypes.*;

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

WHITE_SPACE_CHAR = [\ \n\t\f\r]

LINE_DOCUMENTATION = "/""/""/"[^\n]*
LINE_COMMENT = "/""/"[^\n]*

NUMBER=[0-9]+
IDENTIFIER=[^(){}:;?#$.\^~#\s]+
BITSTRING=(#[0-9a-fA-F]+_?)|(\$[01]*_?)
UINT=uint[0-9]*
BITS=bits[0-9]+

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

\s+ { return WHITE_SPACE; }
{LINE_DOCUMENTATION} { return LINE_DOCUMENTATION; }
{LINE_COMMENT} { return LINE_COMMENT; }
{BITSTRING} { return BITSTRING; }

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

"<=" { return LEQ; }
">=" { return GEQ; }
"<" { return LESS; }
">" { return GREATER; }
"==" { return EQ; }
"!=" { return NEQ; }

"##" { return NAT_WIDTH; }
"#<" { return NAT_LESS; }
"#<=" { return NAT_LEQ; }
"#" { return NAT; }
"Any" { return ANY_KEYWORD; }
"Type" { return TYPE_KEYWORD; }
"Cell" { return CELL_KEYWORD; }
"int" { return INT_KEYWORD; }
{UINT} { return UINT_KEYWORD; }
{BITS} { return BITS_KEYWORD; }

{NUMBER} { return NUMBER; }
{IDENTIFIER} { return IDENTIFIER; }

[^] { return BAD_CHARACTER; }
