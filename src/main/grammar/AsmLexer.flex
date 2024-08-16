package org.ton.intellij.asm.parser;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;

import static com.intellij.psi.TokenType.BAD_CHARACTER;
import static com.intellij.psi.TokenType.WHITE_SPACE;
import static org.ton.intellij.asm.AsmElementTypes.*;

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

INTEGER=[0-9]+
STACK_REGISTER=s[0-9]+|s\(-[0-9]+\)
CONTROL_REGISTER=c[0-7]
UNKNOWN_IDENTIFIER=[a-zA-Z_][a-zA-Z0-9_]*

%%
<YYINITIAL> {
  {WHITE_SPACE}              { return WHITE_SPACE; }

  "NOP"                      { return NOP; }
  "SWAP"                     { return SWAP; }
  "XCHG0"                    { return XCHG0; }
  "XCHG"                     { return XCHG; }
  "PUSHCTR"                  { return PUSHCTR; }
  "POPCTR"                   { return POPCTR; }
  "PUSH"                     { return PUSH; }
  "DUP"                      { return DUP; }
  "OVER"                     { return OVER; }
  "POP"                      { return POP; }
  "DROP"                     { return DROP; }
  "NIP"                      { return NIP; }
  "XCHG3"                    { return XCHG3; }
  "XCHG2"                    { return XCHG2; }
  "XCPU"                     { return XCPU; }
  "PUXC"                     { return PUXC; }
  "PUSH2"                    { return PUSH2; }
  "XCHG3_l"                  { return XCHG3_L; }
  "XC2PU"                    { return XC2PU; }
  "XCPUXC"                   { return XCPUXC; }
  "XCPU2"                    { return XCPU2; }
  "PUXC2"                    { return PUXC2; }
  "PUXCPU"                   { return PUXCPU; }
  "PU2XC"                    { return PU2XC; }
  "PUSH3"                    { return PUSH3; }
  "BLKSWAP"                  { return BLKSWAP; }
  "ROLL"                     { return ROLL; }
  "ROLLREV"                  { return ROLLREV; }
  "ROT2"                     { return ROT2; }
  "ISNULL"                   { return ISNULL; }
  "NULL"                     { return NULL; }
  "PUSHNULL"                 { return PUSHNULL; }
  "TUPLE"                    { return TUPLE; }
  "NIL"                      { return NIL; }
  "SINGLE"                   { return SINGLE; }
  "PAIR"                     { return PAIR; }
  "CONS"                     { return CONS; }
  "TRIPLE"                   { return TRIPLE; }
  "INDEX"                    { return INDEX; }
  "FIRST"                    { return FIRST; }
  "CAR"                      { return CAR; }
  "SECOND"                   { return SECOND; }
  "CDR"                      { return CDR; }
  "THIRD"                    { return THIRD; }
  "UNTUPLE"                  { return UNTUPLE; }
  "UNSINGLE"                 { return UNSINGLE; }
  "UNPAIR"                   { return UNPAIR; }
  "UNCONS"                   { return UNCONS; }
  "UNTRIPLE"                 { return UNTRIPLE; }
  "UNPACKFIRST"              { return UNPACKFIRST; }
  "CHKTUPLE"                 { return CHKTUPLE; }
  "EXPLODE"                  { return EXPLODE; }
  "SETINDEX"                 { return SETINDEX; }
  "SETFIRST"                 { return SETFIRST; }
  "SETSECOND"                { return SETSECOND; }
  "SETTHIRD"                 { return SETTHIRD; }
  "INDEXQ"                   { return INDEXQ; }
  "FIRSTQ"                   { return FIRSTQ; }
  "CARQ"                     { return CARQ; }
  "SECONDQ"                  { return SECONDQ; }
  "CDRQ"                     { return CDRQ; }
  "THIRDQ"                   { return THIRDQ; }
  "SETINDEXQ"                { return SETINDEXQ; }
  "SETFIRSTQ"                { return SETFIRSTQ; }
  "SETSECONDQ"               { return SETSECONDQ; }
  "SETTHIRDQ"                { return SETTHIRDQ; }
  "TUPLEVAR"                 { return TUPLEVAR; }
  "INDEXVAR"                 { return INDEXVAR; }
  "UNTUPLEVAR"               { return UNTUPLEVAR; }
  "UNPACKFIRSTVAR"           { return UNPACKFIRSTVAR; }
  "EXPLODEVAR"               { return EXPLODEVAR; }
  "SETINDEXVAR"              { return SETINDEXVAR; }
  "INDEXVARQ"                { return INDEXVARQ; }
  "SETINDEXVARQ"             { return SETINDEXVARQ; }
  "TLEN"                     { return TLEN; }
  "QTLEN"                    { return QTLEN; }
  "ISTUPLE"                  { return ISTUPLE; }
  "LAST"                     { return LAST; }
  "TPUSH"                    { return TPUSH; }
  "COMMA"                    { return COMMA; }
  "TPOP"                     { return TPOP; }
  "NULLSWAPIF"               { return NULLSWAPIF; }
  "NULLSWAPIFNOT"            { return NULLSWAPIFNOT; }
  "NULLROTRIF"               { return NULLROTRIF; }
  "NULLROTRIFNOT"            { return NULLROTRIFNOT; }
  "NULLSWAPIF2"              { return NULLSWAPIF2; }
  "NULLSWAPIFNOT2"           { return NULLSWAPIFNOT2; }
  "NULLROTRIF2"              { return NULLROTRIF2; }
  "NULLROTRIFNOT2"           { return NULLROTRIFNOT2; }
  "INDEX2"                   { return INDEX2; }
  "CADR"                     { return CADR; }
  "CDDR"                     { return CDDR; }
  "INDEX3"                   { return INDEX3; }
  "CADDR"                    { return CADDR; }
  "CDDDR"                    { return CDDDR; }

  {INTEGER}                  { return INTEGER; }
  {STACK_REGISTER}           { return STACK_REGISTER; }
  {CONTROL_REGISTER}         { return CONTROL_REGISTER; }
  {UNKNOWN_IDENTIFIER}       { return UNKNOWN_IDENTIFIER; }

}

[^] { return BAD_CHARACTER; }
