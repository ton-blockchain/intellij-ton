package org.ton.intellij.asm.lexer;

import com.intellij.psi.tree.IElementType;

import com.intellij.lexer.FlexLexer;
import static com.intellij.psi.TokenType.BAD_CHARACTER;
import static com.intellij.psi.TokenType.WHITE_SPACE;
import static org.ton.intellij.asm.psi.AsmElementTypes.*;

%%

%{
  public _AsmLexer() {
    this((java.io.Reader)null);
  }
%}

%public
%class _AsmLexer
%implements FlexLexer
%function advance
%type IElementType
%unicode

EOL=\R
WHITE_SPACE=\s+

STACK_REGISTER=s[0-9]+|s\(-[0-9]+\)
CONTROL_REGISTER=c[0-7]
UNKNOWN_IDENTIFIER=[a-zA-Z_][a-zA-Z0-9_#]*
NUMBER_DIGIT_LITERAL=(-?[0-9]+("/"-?[0-9]+)?)
NUMBER_HEX_LITERAL=(0[xX][0-9a-fA-F]+)
NUMBER_BINARY_LITERAL=(0[bB][01]+)
SLICE_BINARY_LITERAL=(b\{[01]+})
SLICE_HEX_LITERAL=(x\{[0-9a-fA-F_]+})
WORD=\S+

%%
<YYINITIAL> {
  {WHITE_SPACE}              { return WHITE_SPACE; }
  '<b'                          { return BUILDER_BEGIN; }
  'b>'                          { return BUILDER_END; }
  '<s'                          { return SLICE_BEGIN; }
  's>'                          { return SLICE_END; }
  '<\{'                         { return CODE_BEGIN; }
  '\}>'                         { return CODE_END; }
  '\}>c'                        { return CODE_END_CELL; }
  '\}>s'                        { return CODE_END_SLICE; }
  {NUMBER_DIGIT_LITERAL}                  { return INTEGER; }
  {NUMBER_BINARY_LITERAL}                 { return INTEGER; }
  {NUMBER_HEX_LITERAL}                    { return INTEGER; }
  {SLICE_BINARY_LITERAL}                  { return SLICE_BINARY; }
  {SLICE_HEX_LITERAL}                     { return SLICE_HEX; }
  {STACK_REGISTER}           { return STACK_REGISTER; }
  {CONTROL_REGISTER}         { return CONTROL_REGISTER; }
  {WORD}                     { return WORD; }
}

[^] { return BAD_CHARACTER; }
