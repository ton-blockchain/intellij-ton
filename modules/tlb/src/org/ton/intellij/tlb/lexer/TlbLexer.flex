package org.ton.intellij.tlb.lexer;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.containers.Stack;

import static com.intellij.psi.TokenType.BAD_CHARACTER;
import static com.intellij.psi.TokenType.WHITE_SPACE;
import static org.ton.intellij.tlb.psi.TlbTypes.*;

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
HEX_TAG=#([0-9a-fA-F]+_?|_)
BINARY_TAG=\$([01]*|_)

LINE_DOCUMENTATION = "/""/""/"[^\n]*
LINE_COMMENT = "/""/"[^\n]*

NUMBER=[0-9]+
IDENTIFIER=[0-9a-zA-Z_]*
//PREDIFINED_TYPE=u?int[0-9]+|Cell

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
"!" { return EXCL; }
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

"Type" { return TYPE_KEYWORD; }

{IDENTIFIER} { return IDENTIFIER; }

[^] { return BAD_CHARACTER; }
