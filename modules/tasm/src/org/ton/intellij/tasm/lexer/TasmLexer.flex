package org.ton.intellij.tasm.lexer;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;

import static org.ton.intellij.tasm.psi.TasmTypes.*;

%%

%{
  public TasmLexer() {
    this((java.io.Reader)null);
  }
%}

%public
%class TasmLexer
%implements FlexLexer
%function advance
%type IElementType
%unicode

WHITE_SPACE=[ \t\n\x0B\f\r]+
COMMENT="//"[^\r\n]*

// Token patterns
INTEGER = -{0,1}((0[xX][0-9a-fA-F_]+)|(0[bB][01_]+)|(0[oO][0-7_]+)|([0-9_]+))
HEX = x\{[0-9a-fA-F_]*\}
BIN = b\{[01]*\}
BOC = boc\{[0-9a-fA-F]*\}
STRING = \"[^\"\\]*\"
STACK = s-?[0-9]+
CTRL = c[0-9]+
IDENTIFIER = [a-zA-Z_][a-zA-Z0-9_]*

%%
<YYINITIAL> {
  {WHITE_SPACE}          { return com.intellij.psi.TokenType.WHITE_SPACE; }
  {COMMENT}              { return COMMENT; }

  "ref"                  { return REF; }
  "embed"                { return EMBED; }
  "exotic"               { return EXOTIC; }
  "library"              { return LIBRARY; }

  "{"                    { return LBRACE; }
  "}"                    { return RBRACE; }
  "["                    { return LBRACKET; }
  "]"                    { return RBRACKET; }
  "=>"                   { return ARROW; }

  {INTEGER}              { return INTEGER; }
  {HEX}                  { return HEX; }
  {BIN}                  { return BIN; }
  {BOC}                  { return BOC; }
  {STRING}               { return STRING; }
  {STACK}                { return STACK; }
  {CTRL}                 { return CTRL; }
  {IDENTIFIER}           { return IDENTIFIER; }

  [^]                    { return com.intellij.psi.TokenType.BAD_CHARACTER; }
}