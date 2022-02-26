package com.github.andreypfau.intellijton.fift.parser;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.containers.Stack;

import static com.intellij.psi.TokenType.BAD_CHARACTER;
import static com.intellij.psi.TokenType.WHITE_SPACE;
import static com.github.andreypfau.intellijton.fift.psi.FiftTypes.*;

%%
%unicode
%public
%class _FiftLexer
%implements FlexLexer

%{
    public _FiftLexer() {
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
WHITE_SPACE_CHAR=\s

CHAR="char"\s\S
ABORT=(abort\"([^\"\r\n\\]|\\.)*\")
PRINT=(\.\"([^\"\r\n\\]|\\.)*\")
STRING_CONCAT=(\+\"([^\"\r\n\\]|\\.)*\")
STRING_LITERAL=(\"([^\"\r\n\\]|\\.)*\")
NUMBER_DIGIT_LITERAL=(-?[0-9]+("/"-?[0-9]+)?)
NUMBER_HEX_LITERAL=(0[xX][0-9a-fA-F]+)
NUMBER_BINARY_LITERAL=(0[bB][01]+)
SLICE_BINARY_LITERAL=(b\{[01]+})
SLICE_HEX_LITERAL=(x\{[0-9a-fA-F_]+})
BYTE_HEX_LITERAL=(B\{[0-9a-fA-F_]+})
WORD_DEF=(::_|::|:)\s(\S+)
WHITE_SPACE=[ \t\n\x0B\f\r]+
IDENTIFIER=[^ \t\n\x0B\f\r]+
LINE_COMMENT = "/""/"[^\n]*

%xstate BLOCK_COMMENT_STATE

%%

"[" { return LBRACKET; }
"]" { return RBRACKET; }
"{" { return LBRACE; }
"}" { return RBRACE; }
"(" { return LPAREN; }
")" { return RPAREN; }
"_(" { return UNDERSCORE_LPAREN; }
"dup" { return DUP; }
"drop" { return DROP; }
"swap" { return SWAP; }
"rot" { return ROT; }
"-rot" { return REV_ROT; }
"over" { return OVER; }
"tuck" { return TUCK; }
"nip" { return NIP; }
"2dup" { return DUP_DUP; }
"2drop" { return DROP_DROP; }
"2swap" { return SWAP_SWAP; }
"pick" { return PICK; }
"roll" { return ROLL; }
"-roll" { return REV_ROLL; }
"exch" { return EXCH; }
"exch2" { return EXCH2; }
"?dup" { return COND_DUP; }
"if" { return IF; }
"ifnot" { return IFNOT; }
"cond" { return COND; }
"until" { return UNTIL; }
"while" { return WHILE; }
"times" { return TIMES; }
"include" { return INCLUDE; }
"true" { return TRUE; }
"false" { return FALSE; }

{CHAR} { return CHAR; }
{ABORT} { return ABORT; }
{PRINT} { return PRINT; }
{WORD_DEF} { return WORD_DEF; }
{STRING_CONCAT} { return STRING_CONCAT; }
{STRING_LITERAL} { return STRING_LITERAL; }
{NUMBER_DIGIT_LITERAL} { return NUMBER_DIGIT_LITERAL; }
{NUMBER_HEX_LITERAL} { return NUMBER_HEX_LITERAL; }
{NUMBER_BINARY_LITERAL} { return NUMBER_BINARY_LITERAL; }
{SLICE_BINARY_LITERAL} { return SLICE_BINARY_LITERAL; }
{SLICE_HEX_LITERAL} { return SLICE_HEX_LITERAL; }
{BYTE_HEX_LITERAL} { return BYTE_HEX_LITERAL; }

"/**/" { return BLOCK_COMMENT; }

"/*" {
          pushState(BLOCK_COMMENT_STATE);
          commentDepth = 0;
          commentStart = getTokenStart();
}

<BLOCK_COMMENT_STATE> {
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

{WHITE_SPACE} { return WHITE_SPACE; }
{LINE_COMMENT} { return LINE_COMMENT; }
{IDENTIFIER} { return IDENTIFIER; }
({WHITE_SPACE_CHAR})+ { return WHITE_SPACE; }