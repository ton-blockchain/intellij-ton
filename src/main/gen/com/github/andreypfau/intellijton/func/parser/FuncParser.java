// This is a generated file. Not intended for manual editing.
package com.github.andreypfau.intellijton.func.parser;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilder.Marker;
import static com.github.andreypfau.intellijton.func.psi.FuncTypes.*;
import static com.intellij.lang.parser.GeneratedParserUtilBase.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.TokenSet;
import com.intellij.lang.PsiParser;
import com.intellij.lang.LightPsiParser;

@SuppressWarnings({"SimplifiableIfStatement", "UnusedAssignment"})
public class FuncParser implements PsiParser, LightPsiParser {

  public ASTNode parse(IElementType t, PsiBuilder b) {
    parseLight(t, b);
    return b.getTreeBuilt();
  }

  public void parseLight(IElementType t, PsiBuilder b) {
    boolean r;
    b = adapt_builder_(t, b, this, EXTENDS_SETS_);
    Marker m = enter_section_(b, 0, _COLLAPSE_, null);
    r = parse_root_(t, b);
    exit_section_(b, 0, m, t, r, true, TRUE_CONDITION);
  }

  protected boolean parse_root_(IElementType t, PsiBuilder b) {
    return parse_root_(t, b, 0);
  }

  static boolean parse_root_(IElementType t, PsiBuilder b, int l) {
    return SourceUnit(b, l + 1);
  }

  public static final TokenSet[] EXTENDS_SETS_ = new TokenSet[] {
    create_token_set_(PRIMITIVE_TYPE_NAME, TYPE_NAME),
    create_token_set_(AND_EXPRESSION, AND_OP_EXPRESSION, ASSIGNMENT_EXPRESSION, COMP_EXPRESSION,
      EQ_EXPRESSION, EXPRESSION, FUNCTION_CALL_EXPRESSION, INDEX_ACCESS_EXPRESSION,
      INLINE_ARRAY_EXPRESSION, MEMBER_ACCESS_EXPRESSION, MODIFIER_ACCESS_EXPRESSION, MULT_DIV_EXPRESSION,
      OR_EXPRESSION, OR_OP_EXPRESSION, PLUS_MIN_EXPRESSION, PRIMARY_EXPRESSION,
      SEQ_EXPRESSION, SHIFT_EXPRESSION, TERNARY_EXPRESSION, XOR_OP_EXPRESSION),
  };

  /* ********************************************************** */
  // UnfinishedBlock '}'
  public static boolean Block(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Block")) return false;
    if (!nextTokenIs(b, LBRACE)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = UnfinishedBlock(b, l + 1);
    r = r && consumeToken(b, RBRACE);
    exit_section_(b, m, BLOCK, r);
    return r;
  }

  /* ********************************************************** */
  // booleanLiteral
  public static boolean BooleanLiteral(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "BooleanLiteral")) return false;
    if (!nextTokenIs(b, BOOLEANLITERAL)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, BOOLEANLITERAL);
    exit_section_(b, m, BOOLEAN_LITERAL, r);
    return r;
  }

  /* ********************************************************** */
  // 'global' TypeName Identifier ('=' Expression)? ';'
  public static boolean ConstantVariableDeclaration(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ConstantVariableDeclaration")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, CONSTANT_VARIABLE_DECLARATION, "<constant variable declaration>");
    r = consumeToken(b, "global");
    r = r && TypeName(b, l + 1);
    r = r && consumeToken(b, IDENTIFIER);
    r = r && ConstantVariableDeclaration_3(b, l + 1);
    r = r && consumeToken(b, SEMICOLON);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // ('=' Expression)?
  private static boolean ConstantVariableDeclaration_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ConstantVariableDeclaration_3")) return false;
    ConstantVariableDeclaration_3_0(b, l + 1);
    return true;
  }

  // '=' Expression
  private static boolean ConstantVariableDeclaration_3_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ConstantVariableDeclaration_3_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, ASSIGN);
    r = r && Expression(b, l + 1, -1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // decimalNumber
  static boolean DecimalNumber(PsiBuilder b, int l) {
    return consumeToken(b, DECIMALNUMBER);
  }

  /* ********************************************************** */
  // Identifier ','? | ','
  public static boolean DeclarationItem(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "DeclarationItem")) return false;
    if (!nextTokenIs(b, "<declaration item>", COMMA, IDENTIFIER)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, DECLARATION_ITEM, "<declaration item>");
    r = DeclarationItem_0(b, l + 1);
    if (!r) r = consumeToken(b, COMMA);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // Identifier ','?
  private static boolean DeclarationItem_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "DeclarationItem_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, IDENTIFIER);
    r = r && DeclarationItem_0_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // ','?
  private static boolean DeclarationItem_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "DeclarationItem_0_1")) return false;
    consumeToken(b, COMMA);
    return true;
  }

  /* ********************************************************** */
  // '(' DeclarationItem*  ')'
  public static boolean DeclarationList(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "DeclarationList")) return false;
    if (!nextTokenIs(b, LPAREN)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LPAREN);
    r = r && DeclarationList_1(b, l + 1);
    r = r && consumeToken(b, RPAREN);
    exit_section_(b, m, DECLARATION_LIST, r);
    return r;
  }

  // DeclarationItem*
  private static boolean DeclarationList_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "DeclarationList_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!DeclarationItem(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "DeclarationList_1", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // do Block until '(' Expression ')'
  public static boolean DoUntilStatement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "DoUntilStatement")) return false;
    if (!nextTokenIs(b, DO)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, DO_UNTIL_STATEMENT, null);
    r = consumeToken(b, DO);
    p = r; // pin = 1
    r = r && report_error_(b, Block(b, l + 1));
    r = p && report_error_(b, consumeTokens(b, -1, UNTIL, LPAREN)) && r;
    r = p && report_error_(b, Expression(b, l + 1, -1)) && r;
    r = p && consumeToken(b, RPAREN) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // Expression
  static boolean ExpressionStatement(PsiBuilder b, int l) {
    return Expression(b, l + 1, -1);
  }

  /* ********************************************************** */
  // DoUntilStatement
  //     | ReturnSt
  //     | SimpleStatement
  static boolean FinishedStatement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "FinishedStatement")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_);
    r = DoUntilStatement(b, l + 1);
    if (!r) r = ReturnSt(b, l + 1);
    if (!r) r = SimpleStatement(b, l + 1);
    exit_section_(b, l, m, r, false, FuncParser::UntilSemicolonRecover);
    return r;
  }

  /* ********************************************************** */
  // fixedNumber
  static boolean FixedNumber(PsiBuilder b, int l) {
    return consumeToken(b, FIXEDNUMBER);
  }

  /* ********************************************************** */
  // Expression? ( ',' Expression )*
  public static boolean FunctionCallArguments(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "FunctionCallArguments")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, FUNCTION_CALL_ARGUMENTS, "<function call arguments>");
    r = FunctionCallArguments_0(b, l + 1);
    r = r && FunctionCallArguments_1(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // Expression?
  private static boolean FunctionCallArguments_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "FunctionCallArguments_0")) return false;
    Expression(b, l + 1, -1);
    return true;
  }

  // ( ',' Expression )*
  private static boolean FunctionCallArguments_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "FunctionCallArguments_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!FunctionCallArguments_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "FunctionCallArguments_1", c)) break;
    }
    return true;
  }

  // ',' Expression
  private static boolean FunctionCallArguments_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "FunctionCallArguments_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COMMA);
    r = r && Expression(b, l + 1, -1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // ReturnDef Identifier ParameterList FunctionSpecifiers* ( ';' | Block )
  public static boolean FunctionDefinition(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "FunctionDefinition")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, FUNCTION_DEFINITION, "<function definition>");
    r = ReturnDef(b, l + 1);
    r = r && consumeToken(b, IDENTIFIER);
    p = r; // pin = 2
    r = r && report_error_(b, ParameterList(b, l + 1));
    r = p && report_error_(b, FunctionDefinition_3(b, l + 1)) && r;
    r = p && FunctionDefinition_4(b, l + 1) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // FunctionSpecifiers*
  private static boolean FunctionDefinition_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "FunctionDefinition_3")) return false;
    while (true) {
      int c = current_position_(b);
      if (!FunctionSpecifiers(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "FunctionDefinition_3", c)) break;
    }
    return true;
  }

  // ';' | Block
  private static boolean FunctionDefinition_4(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "FunctionDefinition_4")) return false;
    boolean r;
    r = consumeToken(b, SEMICOLON);
    if (!r) r = Block(b, l + 1);
    return r;
  }

  /* ********************************************************** */
  // '(' FunctionCallArguments? ')'
  static boolean FunctionInvocation(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "FunctionInvocation")) return false;
    if (!nextTokenIs(b, LPAREN)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LPAREN);
    r = r && FunctionInvocation_1(b, l + 1);
    r = r && consumeToken(b, RPAREN);
    exit_section_(b, m, null, r);
    return r;
  }

  // FunctionCallArguments?
  private static boolean FunctionInvocation_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "FunctionInvocation_1")) return false;
    FunctionCallArguments(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // impure | inline | inline_ref | method_id
  public static boolean FunctionSpecifiers(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "FunctionSpecifiers")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, FUNCTION_SPECIFIERS, "<function specifiers>");
    r = consumeToken(b, IMPURE);
    if (!r) r = consumeToken(b, INLINE);
    if (!r) r = consumeToken(b, INLINE_REF);
    if (!r) r = consumeToken(b, METHOD_ID);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // hexLiteral
  static boolean HexLiteral(PsiBuilder b, int l) {
    return consumeToken(b, HEXLITERAL);
  }

  /* ********************************************************** */
  // hexNumber
  static boolean HexNumber(PsiBuilder b, int l) {
    return consumeToken(b, HEXNUMBER);
  }

  /* ********************************************************** */
  // ifnot '(' Expression ')' Block ( else Block )?
  public static boolean IfNotStatement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "IfNotStatement")) return false;
    if (!nextTokenIs(b, IFNOT)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, IF_NOT_STATEMENT, null);
    r = consumeTokens(b, 1, IFNOT, LPAREN);
    p = r; // pin = 1
    r = r && report_error_(b, Expression(b, l + 1, -1));
    r = p && report_error_(b, consumeToken(b, RPAREN)) && r;
    r = p && report_error_(b, Block(b, l + 1)) && r;
    r = p && IfNotStatement_5(b, l + 1) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // ( else Block )?
  private static boolean IfNotStatement_5(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "IfNotStatement_5")) return false;
    IfNotStatement_5_0(b, l + 1);
    return true;
  }

  // else Block
  private static boolean IfNotStatement_5_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "IfNotStatement_5_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, ELSE);
    r = r && Block(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // if '(' Expression ')' Block ( else Block )?
  public static boolean IfStatement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "IfStatement")) return false;
    if (!nextTokenIs(b, IF)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, IF_STATEMENT, null);
    r = consumeTokens(b, 1, IF, LPAREN);
    p = r; // pin = 1
    r = r && report_error_(b, Expression(b, l + 1, -1));
    r = p && report_error_(b, consumeToken(b, RPAREN)) && r;
    r = p && report_error_(b, Block(b, l + 1)) && r;
    r = p && IfStatement_5(b, l + 1) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // ( else Block )?
  private static boolean IfStatement_5(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "IfStatement_5")) return false;
    IfStatement_5_0(b, l + 1);
    return true;
  }

  // else Block
  private static boolean IfStatement_5_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "IfStatement_5_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, ELSE);
    r = r && Block(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // HexNumber | DecimalNumber | FixedNumber | ScientificNumber
  public static boolean NumberLiteral(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "NumberLiteral")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, NUMBER_LITERAL, "<number literal>");
    r = HexNumber(b, l + 1);
    if (!r) r = DecimalNumber(b, l + 1);
    if (!r) r = FixedNumber(b, l + 1);
    if (!r) r = ScientificNumber(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // TypeName Identifier?
  public static boolean ParameterDef(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ParameterDef")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, PARAMETER_DEF, "<parameter def>");
    r = TypeName(b, l + 1);
    r = r && ParameterDef_1(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // Identifier?
  private static boolean ParameterDef_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ParameterDef_1")) return false;
    consumeToken(b, IDENTIFIER);
    return true;
  }

  /* ********************************************************** */
  // '(' ( ParameterDef (',' ParameterDef)* )? ')'
  public static boolean ParameterList(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ParameterList")) return false;
    if (!nextTokenIs(b, LPAREN)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LPAREN);
    r = r && ParameterList_1(b, l + 1);
    r = r && consumeToken(b, RPAREN);
    exit_section_(b, m, PARAMETER_LIST, r);
    return r;
  }

  // ( ParameterDef (',' ParameterDef)* )?
  private static boolean ParameterList_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ParameterList_1")) return false;
    ParameterList_1_0(b, l + 1);
    return true;
  }

  // ParameterDef (',' ParameterDef)*
  private static boolean ParameterList_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ParameterList_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = ParameterDef(b, l + 1);
    r = r && ParameterList_1_0_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (',' ParameterDef)*
  private static boolean ParameterList_1_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ParameterList_1_0_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!ParameterList_1_0_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "ParameterList_1_0_1", c)) break;
    }
    return true;
  }

  // ',' ParameterDef
  private static boolean ParameterList_1_0_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ParameterList_1_0_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COMMA);
    r = r && ParameterDef(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // int | cell | slice | builder | tuple | cont | var
  public static boolean PrimitiveTypeName(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "PrimitiveTypeName")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, PRIMITIVE_TYPE_NAME, "<primitive type name>");
    r = consumeToken(b, INT);
    if (!r) r = consumeToken(b, CELL);
    if (!r) r = consumeToken(b, SLICE);
    if (!r) r = consumeToken(b, BUILDER);
    if (!r) r = consumeToken(b, TUPLE);
    if (!r) r = consumeToken(b, CONT);
    if (!r) r = consumeToken(b, VAR);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // PrimitiveTypeName
  static boolean PrimitiveTypeNameExpression(PsiBuilder b, int l) {
    return PrimitiveTypeName(b, l + 1);
  }

  /* ********************************************************** */
  // repeat '(' Expression ')' Block
  public static boolean RepeatStatement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "RepeatStatement")) return false;
    if (!nextTokenIs(b, REPEAT)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, REPEAT_STATEMENT, null);
    r = consumeTokens(b, 1, REPEAT, LPAREN);
    p = r; // pin = 1
    r = r && report_error_(b, Expression(b, l + 1, -1));
    r = p && report_error_(b, consumeToken(b, RPAREN)) && r;
    r = p && Block(b, l + 1) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // TypeName |
  //     '(' ( TypeName (',' TypeName)* )? ')' |
  //     '[' ( TypeName (',' TypeName)* )? ']' |
  //     '_'
  public static boolean ReturnDef(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ReturnDef")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, RETURN_DEF, "<return def>");
    r = TypeName(b, l + 1);
    if (!r) r = ReturnDef_1(b, l + 1);
    if (!r) r = ReturnDef_2(b, l + 1);
    if (!r) r = consumeToken(b, "_");
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // '(' ( TypeName (',' TypeName)* )? ')'
  private static boolean ReturnDef_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ReturnDef_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LPAREN);
    r = r && ReturnDef_1_1(b, l + 1);
    r = r && consumeToken(b, RPAREN);
    exit_section_(b, m, null, r);
    return r;
  }

  // ( TypeName (',' TypeName)* )?
  private static boolean ReturnDef_1_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ReturnDef_1_1")) return false;
    ReturnDef_1_1_0(b, l + 1);
    return true;
  }

  // TypeName (',' TypeName)*
  private static boolean ReturnDef_1_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ReturnDef_1_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = TypeName(b, l + 1);
    r = r && ReturnDef_1_1_0_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (',' TypeName)*
  private static boolean ReturnDef_1_1_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ReturnDef_1_1_0_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!ReturnDef_1_1_0_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "ReturnDef_1_1_0_1", c)) break;
    }
    return true;
  }

  // ',' TypeName
  private static boolean ReturnDef_1_1_0_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ReturnDef_1_1_0_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COMMA);
    r = r && TypeName(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // '[' ( TypeName (',' TypeName)* )? ']'
  private static boolean ReturnDef_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ReturnDef_2")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LBRACKET);
    r = r && ReturnDef_2_1(b, l + 1);
    r = r && consumeToken(b, RBRACKET);
    exit_section_(b, m, null, r);
    return r;
  }

  // ( TypeName (',' TypeName)* )?
  private static boolean ReturnDef_2_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ReturnDef_2_1")) return false;
    ReturnDef_2_1_0(b, l + 1);
    return true;
  }

  // TypeName (',' TypeName)*
  private static boolean ReturnDef_2_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ReturnDef_2_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = TypeName(b, l + 1);
    r = r && ReturnDef_2_1_0_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (',' TypeName)*
  private static boolean ReturnDef_2_1_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ReturnDef_2_1_0_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!ReturnDef_2_1_0_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "ReturnDef_2_1_0_1", c)) break;
    }
    return true;
  }

  // ',' TypeName
  private static boolean ReturnDef_2_1_0_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ReturnDef_2_1_0_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COMMA);
    r = r && TypeName(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // return Expression?
  public static boolean ReturnSt(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ReturnSt")) return false;
    if (!nextTokenIs(b, RETURN)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, RETURN_ST, null);
    r = consumeToken(b, RETURN);
    p = r; // pin = 1
    r = r && ReturnSt_1(b, l + 1);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // Expression?
  private static boolean ReturnSt_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ReturnSt_1")) return false;
    Expression(b, l + 1, -1);
    return true;
  }

  /* ********************************************************** */
  // return SeqExpression ';'
  public static boolean ReturnTupleStatement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ReturnTupleStatement")) return false;
    if (!nextTokenIs(b, RETURN)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, RETURN);
    r = r && SeqExpression(b, l + 1);
    r = r && consumeToken(b, SEMICOLON);
    exit_section_(b, m, RETURN_TUPLE_STATEMENT, r);
    return r;
  }

  /* ********************************************************** */
  // Expression ':' Expression
  static boolean RightTernaryExpression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "RightTernaryExpression")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = Expression(b, l + 1, -1);
    r = r && consumeToken(b, COLON);
    r = r && Expression(b, l + 1, -1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // scientificNumber
  static boolean ScientificNumber(PsiBuilder b, int l) {
    return consumeToken(b, SCIENTIFICNUMBER);
  }

  /* ********************************************************** */
  // VariableDefinition | ExpressionStatement
  static boolean SimpleStatement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "SimpleStatement")) return false;
    boolean r;
    r = VariableDefinition(b, l + 1);
    if (!r) r = ExpressionStatement(b, l + 1);
    return r;
  }

  /* ********************************************************** */
  // (
  //     FunctionDefinition
  //     | ConstantVariableDeclaration
  //     | Expression
  //   )*
  static boolean SourceUnit(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "SourceUnit")) return false;
    while (true) {
      int c = current_position_(b);
      if (!SourceUnit_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "SourceUnit", c)) break;
    }
    return true;
  }

  // FunctionDefinition
  //     | ConstantVariableDeclaration
  //     | Expression
  private static boolean SourceUnit_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "SourceUnit_0")) return false;
    boolean r;
    r = FunctionDefinition(b, l + 1);
    if (!r) r = ConstantVariableDeclaration(b, l + 1);
    if (!r) r = Expression(b, l + 1, -1);
    return r;
  }

  /* ********************************************************** */
  // IfStatement | IfNotStatement | WhileStatement | RepeatStatement | Block | ReturnTupleStatement | FinishedStatement ';'
  public static boolean Statement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Statement")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, STATEMENT, "<statement>");
    r = IfStatement(b, l + 1);
    if (!r) r = IfNotStatement(b, l + 1);
    if (!r) r = WhileStatement(b, l + 1);
    if (!r) r = RepeatStatement(b, l + 1);
    if (!r) r = Block(b, l + 1);
    if (!r) r = ReturnTupleStatement(b, l + 1);
    if (!r) r = Statement_6(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // FinishedStatement ';'
  private static boolean Statement_6(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Statement_6")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = FinishedStatement(b, l + 1);
    r = r && consumeToken(b, SEMICOLON);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // stringLiteral
  static boolean StringLiteral(PsiBuilder b, int l) {
    return consumeToken(b, STRINGLITERAL);
  }

  /* ********************************************************** */
  // PrimitiveTypeName
  public static boolean TypeName(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "TypeName")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _COLLAPSE_, TYPE_NAME, "<type name>");
    r = PrimitiveTypeName(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // (TypeName)? Identifier ','? | ','
  public static boolean TypedDeclarationItem(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "TypedDeclarationItem")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, TYPED_DECLARATION_ITEM, "<typed declaration item>");
    r = TypedDeclarationItem_0(b, l + 1);
    if (!r) r = consumeToken(b, COMMA);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // (TypeName)? Identifier ','?
  private static boolean TypedDeclarationItem_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "TypedDeclarationItem_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = TypedDeclarationItem_0_0(b, l + 1);
    r = r && consumeToken(b, IDENTIFIER);
    r = r && TypedDeclarationItem_0_2(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (TypeName)?
  private static boolean TypedDeclarationItem_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "TypedDeclarationItem_0_0")) return false;
    TypedDeclarationItem_0_0_0(b, l + 1);
    return true;
  }

  // (TypeName)
  private static boolean TypedDeclarationItem_0_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "TypedDeclarationItem_0_0_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = TypeName(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // ','?
  private static boolean TypedDeclarationItem_0_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "TypedDeclarationItem_0_2")) return false;
    consumeToken(b, COMMA);
    return true;
  }

  /* ********************************************************** */
  // '(' TypedDeclarationItem*  ')'
  public static boolean TypedDeclarationList(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "TypedDeclarationList")) return false;
    if (!nextTokenIs(b, LPAREN)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LPAREN);
    r = r && TypedDeclarationList_1(b, l + 1);
    r = r && consumeToken(b, RPAREN);
    exit_section_(b, m, TYPED_DECLARATION_LIST, r);
    return r;
  }

  // TypedDeclarationItem*
  private static boolean TypedDeclarationList_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "TypedDeclarationList_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!TypedDeclarationItem(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "TypedDeclarationList_1", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // TypeName ','? | ','
  public static boolean TypedItem(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "TypedItem")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, TYPED_ITEM, "<typed item>");
    r = TypedItem_0(b, l + 1);
    if (!r) r = consumeToken(b, COMMA);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // TypeName ','?
  private static boolean TypedItem_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "TypedItem_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = TypeName(b, l + 1);
    r = r && TypedItem_0_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // ','?
  private static boolean TypedItem_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "TypedItem_0_1")) return false;
    consumeToken(b, COMMA);
    return true;
  }

  /* ********************************************************** */
  // '(' TypedItem* ')'
  public static boolean TypedList(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "TypedList")) return false;
    if (!nextTokenIs(b, LPAREN)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LPAREN);
    r = r && TypedList_1(b, l + 1);
    r = r && consumeToken(b, RPAREN);
    exit_section_(b, m, TYPED_LIST, r);
    return r;
  }

  // TypedItem*
  private static boolean TypedList_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "TypedList_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!TypedItem(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "TypedList_1", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // '{' (Statement)*
  static boolean UnfinishedBlock(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "UnfinishedBlock")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_);
    r = consumeToken(b, LBRACE);
    p = r; // pin = 1
    r = r && UnfinishedBlock_1(b, l + 1);
    exit_section_(b, l, m, r, p, FuncParser::UntilBraceRecover);
    return r || p;
  }

  // (Statement)*
  private static boolean UnfinishedBlock_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "UnfinishedBlock_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!UnfinishedBlock_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "UnfinishedBlock_1", c)) break;
    }
    return true;
  }

  // (Statement)
  private static boolean UnfinishedBlock_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "UnfinishedBlock_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = Statement(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // !('}')
  static boolean UntilBraceRecover(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "UntilBraceRecover")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !consumeToken(b, RBRACE);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // !(';')
  static boolean UntilSemicolonRecover(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "UntilSemicolonRecover")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !consumeToken(b, SEMICOLON);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // Identifier
  public static boolean VarLiteral(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "VarLiteral")) return false;
    if (!nextTokenIs(b, IDENTIFIER)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, IDENTIFIER);
    exit_section_(b, m, VAR_LITERAL, r);
    return r;
  }

  /* ********************************************************** */
  // DeclarationList |
  //     TypedDeclarationList |
  //     TypedList Identifier |
  //     TypeName Identifier
  public static boolean VariableDeclaration(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "VariableDeclaration")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, VARIABLE_DECLARATION, "<variable declaration>");
    r = DeclarationList(b, l + 1);
    if (!r) r = TypedDeclarationList(b, l + 1);
    if (!r) r = VariableDeclaration_2(b, l + 1);
    if (!r) r = VariableDeclaration_3(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // TypedList Identifier
  private static boolean VariableDeclaration_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "VariableDeclaration_2")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = TypedList(b, l + 1);
    r = r && consumeToken(b, IDENTIFIER);
    exit_section_(b, m, null, r);
    return r;
  }

  // TypeName Identifier
  private static boolean VariableDeclaration_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "VariableDeclaration_3")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = TypeName(b, l + 1);
    r = r && consumeToken(b, IDENTIFIER);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // VariableDeclaration ( '=' Expression | SeqExpression )?
  public static boolean VariableDefinition(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "VariableDefinition")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, VARIABLE_DEFINITION, "<variable definition>");
    r = VariableDeclaration(b, l + 1);
    r = r && VariableDefinition_1(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // ( '=' Expression | SeqExpression )?
  private static boolean VariableDefinition_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "VariableDefinition_1")) return false;
    VariableDefinition_1_0(b, l + 1);
    return true;
  }

  // '=' Expression | SeqExpression
  private static boolean VariableDefinition_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "VariableDefinition_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = VariableDefinition_1_0_0(b, l + 1);
    if (!r) r = SeqExpression(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // '=' Expression
  private static boolean VariableDefinition_1_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "VariableDefinition_1_0_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, ASSIGN);
    r = r && Expression(b, l + 1, -1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // while '(' Expression ')' Block
  public static boolean WhileStatement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "WhileStatement")) return false;
    if (!nextTokenIs(b, WHILE)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, WHILE_STATEMENT, null);
    r = consumeTokens(b, 1, WHILE, LPAREN);
    p = r; // pin = 1
    r = r && report_error_(b, Expression(b, l + 1, -1));
    r = p && report_error_(b, consumeToken(b, RPAREN)) && r;
    r = p && Block(b, l + 1) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // Expression root: Expression
  // Operator priority table:
  // 0: POSTFIX(TernaryExpression)
  // 1: BINARY(OrExpression)
  // 2: BINARY(AndExpression)
  // 3: BINARY(CompExpression)
  // 4: BINARY(OrOpExpression)
  // 5: BINARY(XorOpExpression)
  // 6: BINARY(AndOpExpression)
  // 7: BINARY(EqExpression)
  // 8: BINARY(ShiftExpression)
  // 9: BINARY(MultDivExpression)
  // 10: POSTFIX(IndexAccessExpression) POSTFIX(MemberAccessExpression) POSTFIX(ModifierAccessExpression) POSTFIX(FunctionCallExpression)
  //    ATOM(SeqExpression)
  // 11: BINARY(AssignmentExpression)
  // 12: BINARY(PlusMinExpression)
  // 13: ATOM(InlineArrayExpression)
  // 14: ATOM(PrimaryExpression)
  public static boolean Expression(PsiBuilder b, int l, int g) {
    if (!recursion_guard_(b, l, "Expression")) return false;
    addVariant(b, "<expression>");
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, "<expression>");
    r = SeqExpression(b, l + 1);
    if (!r) r = InlineArrayExpression(b, l + 1);
    if (!r) r = PrimaryExpression(b, l + 1);
    p = r;
    r = r && Expression_0(b, l + 1, g);
    exit_section_(b, l, m, null, r, p, null);
    return r || p;
  }

  public static boolean Expression_0(PsiBuilder b, int l, int g) {
    if (!recursion_guard_(b, l, "Expression_0")) return false;
    boolean r = true;
    while (true) {
      Marker m = enter_section_(b, l, _LEFT_, null);
      if (g < 0 && TernaryExpression_0(b, l + 1)) {
        r = true;
        exit_section_(b, l, m, TERNARY_EXPRESSION, r, true, null);
      }
      else if (g < 1 && consumeTokenSmart(b, OROR)) {
        r = Expression(b, l, 1);
        exit_section_(b, l, m, OR_EXPRESSION, r, true, null);
      }
      else if (g < 2 && consumeTokenSmart(b, ANDAND)) {
        r = Expression(b, l, 2);
        exit_section_(b, l, m, AND_EXPRESSION, r, true, null);
      }
      else if (g < 3 && CompExpression_0(b, l + 1)) {
        r = Expression(b, l, 3);
        exit_section_(b, l, m, COMP_EXPRESSION, r, true, null);
      }
      else if (g < 4 && consumeTokenSmart(b, OR)) {
        r = Expression(b, l, 4);
        exit_section_(b, l, m, OR_OP_EXPRESSION, r, true, null);
      }
      else if (g < 5 && consumeTokenSmart(b, CARET)) {
        r = Expression(b, l, 5);
        exit_section_(b, l, m, XOR_OP_EXPRESSION, r, true, null);
      }
      else if (g < 6 && consumeTokenSmart(b, AND)) {
        r = Expression(b, l, 6);
        exit_section_(b, l, m, AND_OP_EXPRESSION, r, true, null);
      }
      else if (g < 7 && EqExpression_0(b, l + 1)) {
        r = Expression(b, l, 7);
        exit_section_(b, l, m, EQ_EXPRESSION, r, true, null);
      }
      else if (g < 8 && ShiftExpression_0(b, l + 1)) {
        r = Expression(b, l, 8);
        exit_section_(b, l, m, SHIFT_EXPRESSION, r, true, null);
      }
      else if (g < 9 && MultDivExpression_0(b, l + 1)) {
        r = Expression(b, l, 9);
        exit_section_(b, l, m, MULT_DIV_EXPRESSION, r, true, null);
      }
      else if (g < 10 && IndexAccessExpression_0(b, l + 1)) {
        r = true;
        exit_section_(b, l, m, INDEX_ACCESS_EXPRESSION, r, true, null);
      }
      else if (g < 10 && parseTokensSmart(b, 0, DOT, IDENTIFIER)) {
        r = true;
        exit_section_(b, l, m, MEMBER_ACCESS_EXPRESSION, r, true, null);
      }
      else if (g < 10 && parseTokensSmart(b, 0, TILDE, IDENTIFIER)) {
        r = true;
        exit_section_(b, l, m, MODIFIER_ACCESS_EXPRESSION, r, true, null);
      }
      else if (g < 10 && FunctionInvocation(b, l + 1)) {
        r = true;
        exit_section_(b, l, m, FUNCTION_CALL_EXPRESSION, r, true, null);
      }
      else if (g < 11 && AssignmentExpression_0(b, l + 1)) {
        r = Expression(b, l, 11);
        exit_section_(b, l, m, ASSIGNMENT_EXPRESSION, r, true, null);
      }
      else if (g < 12 && PlusMinExpression_0(b, l + 1)) {
        r = Expression(b, l, 12);
        exit_section_(b, l, m, PLUS_MIN_EXPRESSION, r, true, null);
      }
      else {
        exit_section_(b, l, m, null, false, false, null);
        break;
      }
    }
    return r;
  }

  // '?' RightTernaryExpression
  private static boolean TernaryExpression_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "TernaryExpression_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokenSmart(b, QUESTION);
    r = r && RightTernaryExpression(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // '<' | '>' | '<=' | '>='
  private static boolean CompExpression_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "CompExpression_0")) return false;
    boolean r;
    r = consumeTokenSmart(b, LESS);
    if (!r) r = consumeTokenSmart(b, MORE);
    if (!r) r = consumeTokenSmart(b, LESSEQ);
    if (!r) r = consumeTokenSmart(b, MOREEQ);
    return r;
  }

  // '==' | '!='
  private static boolean EqExpression_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "EqExpression_0")) return false;
    boolean r;
    r = consumeTokenSmart(b, EQ);
    if (!r) r = consumeTokenSmart(b, NEQ);
    return r;
  }

  // '<<' | '>>'
  private static boolean ShiftExpression_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ShiftExpression_0")) return false;
    boolean r;
    r = consumeTokenSmart(b, LSHIFT);
    if (!r) r = consumeTokenSmart(b, RSHIFT);
    return r;
  }

  // '*' | '/' | '%'
  private static boolean MultDivExpression_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "MultDivExpression_0")) return false;
    boolean r;
    r = consumeTokenSmart(b, MULT);
    if (!r) r = consumeTokenSmart(b, DIV);
    if (!r) r = consumeTokenSmart(b, PERCENT);
    return r;
  }

  // '[' ( Expression? ':' Expression? | Expression? ) ']'
  private static boolean IndexAccessExpression_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "IndexAccessExpression_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokenSmart(b, LBRACKET);
    r = r && IndexAccessExpression_0_1(b, l + 1);
    r = r && consumeToken(b, RBRACKET);
    exit_section_(b, m, null, r);
    return r;
  }

  // Expression? ':' Expression? | Expression?
  private static boolean IndexAccessExpression_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "IndexAccessExpression_0_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = IndexAccessExpression_0_1_0(b, l + 1);
    if (!r) r = IndexAccessExpression_0_1_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // Expression? ':' Expression?
  private static boolean IndexAccessExpression_0_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "IndexAccessExpression_0_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = IndexAccessExpression_0_1_0_0(b, l + 1);
    r = r && consumeToken(b, COLON);
    r = r && IndexAccessExpression_0_1_0_2(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // Expression?
  private static boolean IndexAccessExpression_0_1_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "IndexAccessExpression_0_1_0_0")) return false;
    Expression(b, l + 1, -1);
    return true;
  }

  // Expression?
  private static boolean IndexAccessExpression_0_1_0_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "IndexAccessExpression_0_1_0_2")) return false;
    Expression(b, l + 1, -1);
    return true;
  }

  // Expression?
  private static boolean IndexAccessExpression_0_1_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "IndexAccessExpression_0_1_1")) return false;
    Expression(b, l + 1, -1);
    return true;
  }

  // '(' (Expression ',')* Expression? ','* ')'
  public static boolean SeqExpression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "SeqExpression")) return false;
    if (!nextTokenIsSmart(b, LPAREN)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokenSmart(b, LPAREN);
    r = r && SeqExpression_1(b, l + 1);
    r = r && SeqExpression_2(b, l + 1);
    r = r && SeqExpression_3(b, l + 1);
    r = r && consumeToken(b, RPAREN);
    exit_section_(b, m, SEQ_EXPRESSION, r);
    return r;
  }

  // (Expression ',')*
  private static boolean SeqExpression_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "SeqExpression_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!SeqExpression_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "SeqExpression_1", c)) break;
    }
    return true;
  }

  // Expression ','
  private static boolean SeqExpression_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "SeqExpression_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = Expression(b, l + 1, -1);
    r = r && consumeToken(b, COMMA);
    exit_section_(b, m, null, r);
    return r;
  }

  // Expression?
  private static boolean SeqExpression_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "SeqExpression_2")) return false;
    Expression(b, l + 1, -1);
    return true;
  }

  // ','*
  private static boolean SeqExpression_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "SeqExpression_3")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeTokenSmart(b, COMMA)) break;
      if (!empty_element_parsed_guard_(b, "SeqExpression_3", c)) break;
    }
    return true;
  }

  // '=' | '|=' | '^=' | '&=' | '<<=' | '>>=' | '+=' | '-=' | '*=' | '/=' | '%='
  private static boolean AssignmentExpression_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "AssignmentExpression_0")) return false;
    boolean r;
    r = consumeTokenSmart(b, ASSIGN);
    if (!r) r = consumeTokenSmart(b, OR_ASSIGN);
    if (!r) r = consumeTokenSmart(b, XOR_ASSIGN);
    if (!r) r = consumeTokenSmart(b, AND_ASSIGN);
    if (!r) r = consumeTokenSmart(b, LSHIFT_ASSIGN);
    if (!r) r = consumeTokenSmart(b, RSHIFT_ASSIGN);
    if (!r) r = consumeTokenSmart(b, PLUS_ASSIGN);
    if (!r) r = consumeTokenSmart(b, MINUS_ASSIGN);
    if (!r) r = consumeTokenSmart(b, MULT_ASSIGN);
    if (!r) r = consumeTokenSmart(b, DIV_ASSIGN);
    if (!r) r = consumeTokenSmart(b, PERCENT_ASSIGN);
    return r;
  }

  // '+' | '-'
  private static boolean PlusMinExpression_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "PlusMinExpression_0")) return false;
    boolean r;
    r = consumeTokenSmart(b, PLUS);
    if (!r) r = consumeTokenSmart(b, MINUS);
    return r;
  }

  // '[' Expression (',' Expression)* ']'
  public static boolean InlineArrayExpression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "InlineArrayExpression")) return false;
    if (!nextTokenIsSmart(b, LBRACKET)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokenSmart(b, LBRACKET);
    r = r && Expression(b, l + 1, -1);
    r = r && InlineArrayExpression_2(b, l + 1);
    r = r && consumeToken(b, RBRACKET);
    exit_section_(b, m, INLINE_ARRAY_EXPRESSION, r);
    return r;
  }

  // (',' Expression)*
  private static boolean InlineArrayExpression_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "InlineArrayExpression_2")) return false;
    while (true) {
      int c = current_position_(b);
      if (!InlineArrayExpression_2_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "InlineArrayExpression_2", c)) break;
    }
    return true;
  }

  // ',' Expression
  private static boolean InlineArrayExpression_2_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "InlineArrayExpression_2_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokenSmart(b, COMMA);
    r = r && Expression(b, l + 1, -1);
    exit_section_(b, m, null, r);
    return r;
  }

  // VarLiteral
  //                   | BooleanLiteral
  //                   | NumberLiteral
  //                   | HexLiteral
  //                   | StringLiteral
  //                   | PrimitiveTypeNameExpression
  public static boolean PrimaryExpression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "PrimaryExpression")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, PRIMARY_EXPRESSION, "<primary expression>");
    r = VarLiteral(b, l + 1);
    if (!r) r = BooleanLiteral(b, l + 1);
    if (!r) r = NumberLiteral(b, l + 1);
    if (!r) r = HexLiteral(b, l + 1);
    if (!r) r = StringLiteral(b, l + 1);
    if (!r) r = PrimitiveTypeNameExpression(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

}
