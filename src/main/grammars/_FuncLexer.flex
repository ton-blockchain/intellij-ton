package com.github.andreypfau.intellijton.func.parser;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;

import static com.intellij.psi.TokenType.BAD_CHARACTER;
import static com.intellij.psi.TokenType.WHITE_SPACE;
import static com.github.andreypfau.intellijton.func.psi.FuncTypes.*;

%%

%{
  public _FuncLexer() {
    this((java.io.Reader)null);
  }
%}

%public
%class _FuncLexer
%implements FlexLexer
%function advance
%type IElementType
%unicode

EOL=\R
WHITE_SPACE=\s+

COMMENT=(;;.*)|(\{-[\s\S]*?-})
STRINGLITERAL=(\"([^\"\r\n\\]|\\.)*\")|unicode(\"([^\"])*\")
DECIMALNUMBER=([0-9][_0-9]*)
FIXEDNUMBER=(([0-9][_0-9]*)+\.[_0-9]*|([0-9][_0-9]*)*\.([0-9][_0-9]*))
SCIENTIFICNUMBER=((([0-9][_0-9]*)+|([0-9][_0-9]*)+\.[_0-9]*|([0-9][_0-9]*|[0-9])*\.[_0-9]+)[Ee][+-]?[_0-9]+)
HEXNUMBER=(0[xX][_0-9a-fA-F]+)
BOOLEANLITERAL=true|false
SPACE=[ \t\n\x0B\f\r]+
IDENTIFIER=[a-zA-Z_$][a-zA-Z_$0-9]*
NAT_SPEC_TAG=@[a-zA-Z_0-9:]*

%%
<YYINITIAL> {
  {WHITE_SPACE}           { return WHITE_SPACE; }

  "."                     { return DOT; }
  ":"                     { return COLON; }
  ";"                     { return SEMICOLON; }
  ","                     { return COMMA; }
  "+"                     { return PLUS; }
  "-"                     { return MINUS; }
  "*"                     { return MULT; }
  "/"                     { return DIV; }
  "**"                    { return EXPONENT; }
  "!"                     { return NOT; }
  "="                     { return ASSIGN; }
  "=>"                    { return TO; }
  "=="                    { return EQ; }
  "!="                    { return NEQ; }
  "++"                    { return INC; }
  "--"                    { return DEC; }
  "+="                    { return PLUS_ASSIGN; }
  "-="                    { return MINUS_ASSIGN; }
  "*="                    { return MULT_ASSIGN; }
  "/="                    { return DIV_ASSIGN; }
  "|="                    { return OR_ASSIGN; }
  "^="                    { return XOR_ASSIGN; }
  "&="                    { return AND_ASSIGN; }
  "<<="                   { return LSHIFT_ASSIGN; }
  ">>="                   { return RSHIFT_ASSIGN; }
  "%="                    { return PERCENT_ASSIGN; }
  "<"                     { return LESS; }
  "<="                    { return LESSEQ; }
  ">"                     { return MORE; }
  ">="                    { return MOREEQ; }
  "^"                     { return CARET; }
  "&"                     { return AND; }
  "&&"                    { return ANDAND; }
  "|"                     { return OR; }
  "||"                    { return OROR; }
  "["                     { return LBRACKET; }
  "]"                     { return RBRACKET; }
  "{"                     { return LBRACE; }
  "}"                     { return RBRACE; }
  "("                     { return LPAREN; }
  ")"                     { return RPAREN; }
  "?"                     { return QUESTION; }
  "%"                     { return PERCENT; }
  "~"                     { return TILDE; }
  "<<"                    { return LSHIFT; }
  ">>"                    { return RSHIFT; }
  ":="                    { return LEFT_ASSEMBLY; }
  "=:"                    { return RIGHT_ASSEMBLY; }
  "impure"                { return IMPURE; }
  "inline"                { return INLINE; }
  "inline_ref"            { return INLINE_REF; }
  "method_id"             { return METHOD_ID; }
  "return"                { return RETURN; }
  "if"                    { return IF; }
  "else"                  { return ELSE; }
  "ifnot"                 { return IFNOT; }
  "while"                 { return WHILE; }
  "repeat"                { return REPEAT; }
  "do"                    { return DO; }
  "until"                 { return UNTIL; }
  "hexLiteral"            { return HEXLITERAL; }
  "int"                   { return INT; }
  "cell"                  { return CELL; }
  "slice"                 { return SLICE; }
  "builder"               { return BUILDER; }
  "tuple"                 { return TUPLE; }
  "cont"                  { return CONT; }
  "var"                   { return VAR; }

  {COMMENT}               { return COMMENT; }
  {STRINGLITERAL}         { return STRINGLITERAL; }
  {DECIMALNUMBER}         { return DECIMALNUMBER; }
  {FIXEDNUMBER}           { return FIXEDNUMBER; }
  {SCIENTIFICNUMBER}      { return SCIENTIFICNUMBER; }
  {HEXNUMBER}             { return HEXNUMBER; }
  {BOOLEANLITERAL}        { return BOOLEANLITERAL; }
  {SPACE}                 { return SPACE; }
  {IDENTIFIER}            { return IDENTIFIER; }
  {NAT_SPEC_TAG}          { return NAT_SPEC_TAG; }

}

[^] { return BAD_CHARACTER; }
