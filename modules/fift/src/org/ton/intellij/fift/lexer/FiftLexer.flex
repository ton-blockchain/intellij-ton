package org.ton.intellij.fift.lexer;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.containers.Stack;

import static com.intellij.psi.TokenType.BAD_CHARACTER;
import static com.intellij.psi.TokenType.WHITE_SPACE;
import static org.ton.intellij.fift.psi.FiftTypes.*;

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
%xstate ASM_STATE

%%
<YYINITIAL> "PROGRAM{""\n"          { yybegin(ASM_STATE); return PROGRAM_START; }

<ASM_STATE> "}END>c"            { yybegin(YYINITIAL); return END_C; }

<ASM_STATE> ">DO<"              { return DO_SEP; }
<ASM_STATE> "}>DO<{"            { return DO_SEP; }
<ASM_STATE> "}>CONT"            { return ANGLE_RBRACE_CONT; }
<ASM_STATE> "}>c"               { return ANGLE_RBRACE_C; }
<ASM_STATE> "}>s"               { return ANGLE_RBRACE_S; }
<ASM_STATE> "}>"                { return ANGLE_RBRACE; }
<ASM_STATE> "<{"                { return ANGLE_LBRACE; }
<ASM_STATE> "CONT:<{"           { return CONT_START; }

<ASM_STATE> "PROCINLINE:<{"     { return PROCINLINE_START; }
<ASM_STATE> "PROCREF:<{"        { return PROCREF_START; }
<ASM_STATE> "PROC:<{"           { return PROC_START; }
<ASM_STATE> "METHOD:<{"         { return METHOD_START; }

<ASM_STATE> "IFJMP:<{"          { return IFJMP_START; }
<ASM_STATE> "IFNOTJMP:<{"       { return IFNOTJMP_START; }
<ASM_STATE> "IF:<{"             { return IF_START; }
<ASM_STATE> "IFNOT:<{"          { return IFNOT_START; }
<ASM_STATE> "}>ELSE<{"          { return ELSE_START; }
<ASM_STATE> "WHILE:<{"          { return WHILE_START; }
<ASM_STATE> "REPEAT:<{"         { return REPEAT_START; }
<ASM_STATE> "UNTIL:<{"          { return UNTIL_START; }

<ASM_STATE> "DECLPROC"          { return DECLPROC; }
<ASM_STATE> "DECLMETHOD"        { return DECLMETHOD; }
<ASM_STATE> "DECLGLOBVAR"       { return DECLGLOBVAR; }

<YYINITIAL,ASM_STATE> "["         { return LBRACKET; }
<YYINITIAL,ASM_STATE> "]"         { return RBRACKET; }
<YYINITIAL,ASM_STATE> "{"         { return LBRACE; }
<YYINITIAL,ASM_STATE> "}"         { return RBRACE; }
<YYINITIAL,ASM_STATE> "("         { return LPAREN; }
<YYINITIAL,ASM_STATE> ")"         { return RPAREN; }
<YYINITIAL,ASM_STATE> "_("        { return UNDERSCORE_LPAREN; }
<YYINITIAL,ASM_STATE> "dup"       { return DUP; }
<YYINITIAL,ASM_STATE> "drop"      { return DROP; }
<YYINITIAL,ASM_STATE> "swap"      { return SWAP; }
<YYINITIAL,ASM_STATE> "rot"       { return ROT; }
<YYINITIAL,ASM_STATE> "-rot"      { return REV_ROT; }
<YYINITIAL,ASM_STATE> "over"      { return OVER; }
<YYINITIAL,ASM_STATE> "tuck"      { return TUCK; }
<YYINITIAL,ASM_STATE> "nip"       { return NIP; }
<YYINITIAL,ASM_STATE> "2dup"      { return DUP_DUP; }
<YYINITIAL,ASM_STATE> "2drop"     { return DROP_DROP; }
<YYINITIAL,ASM_STATE> "2swap"     { return SWAP_SWAP; }
<YYINITIAL,ASM_STATE> "pick"      { return PICK; }
<YYINITIAL,ASM_STATE> "roll"      { return ROLL; }
<YYINITIAL,ASM_STATE> "-roll"     { return REV_ROLL; }
<YYINITIAL,ASM_STATE> "exch"      { return EXCH; }
<YYINITIAL,ASM_STATE> "exch2"     { return EXCH2; }
<YYINITIAL,ASM_STATE> "?dup"      { return COND_DUP; }
<YYINITIAL,ASM_STATE> "if"        { return IF; }
<YYINITIAL,ASM_STATE> "ifnot"     { return IFNOT; }
<YYINITIAL,ASM_STATE> "cond"      { return COND; }
<YYINITIAL,ASM_STATE> "until"     { return UNTIL; }
<YYINITIAL,ASM_STATE> "while"     { return WHILE; }
<YYINITIAL,ASM_STATE> "times"     { return TIMES; }
<YYINITIAL,ASM_STATE> "include"   { return INCLUDE; }
<YYINITIAL,ASM_STATE> "true"      { return TRUE; }
<YYINITIAL,ASM_STATE> "false"     { return FALSE; }

<YYINITIAL,ASM_STATE> {CHAR}                   { return CHAR; }
<YYINITIAL,ASM_STATE> {ABORT}                  { return ABORT; }
<YYINITIAL,ASM_STATE> {PRINT}                  { return PRINT; }
<YYINITIAL,ASM_STATE> {WORD_DEF}               { return WORD_DEF; }
<YYINITIAL,ASM_STATE> {STRING_CONCAT}          { return STRING_CONCAT; }
<YYINITIAL,ASM_STATE> {STRING_LITERAL}         { return STRING_LITERAL; }
<YYINITIAL,ASM_STATE> {NUMBER_DIGIT_LITERAL}   { return NUMBER_DIGIT_LITERAL; }
<YYINITIAL,ASM_STATE> {NUMBER_HEX_LITERAL}     { return NUMBER_HEX_LITERAL; }
<YYINITIAL,ASM_STATE> {NUMBER_BINARY_LITERAL}  { return NUMBER_BINARY_LITERAL; }
<YYINITIAL,ASM_STATE> {SLICE_BINARY_LITERAL}   { return SLICE_BINARY_LITERAL; }
<YYINITIAL,ASM_STATE> {SLICE_HEX_LITERAL}      { return SLICE_HEX_LITERAL; }
<YYINITIAL,ASM_STATE> {BYTE_HEX_LITERAL}       { return BYTE_HEX_LITERAL; }

<YYINITIAL,ASM_STATE> "/**/"                   { return BLOCK_COMMENT; }
<YYINITIAL,ASM_STATE> "/*"                     { pushState(BLOCK_COMMENT_STATE); commentDepth = 0; commentStart = getTokenStart(); }

<BLOCK_COMMENT_STATE> "/*"                     { commentDepth++; }
<BLOCK_COMMENT_STATE> "*/"                     { if (commentDepth-- == 0) { int s = yystate(); popState(); zzStartRead = commentStart; return commentStateToTokenType(s); } }
<BLOCK_COMMENT_STATE> <<EOF>>                  { int s = yystate(); popState(); zzStartRead = commentStart; return commentStateToTokenType(s); }
<BLOCK_COMMENT_STATE> [\s\S]                   { /* eat */ }

<YYINITIAL,ASM_STATE> {LINE_COMMENT}            { return LINE_COMMENT; }

<YYINITIAL,ASM_STATE> {WHITE_SPACE}             { return WHITE_SPACE; }
<YYINITIAL,ASM_STATE> {IDENTIFIER}              { return IDENTIFIER; }
