// This is a generated file. Not intended for manual editing.
package com.github.andreypfau.intellijton.func.psi;

import com.intellij.psi.tree.IElementType;
import com.intellij.psi.PsiElement;
import com.intellij.lang.ASTNode;
import com.github.andreypfau.intellijton.func.psi.impl.*;

public interface FuncTypes {

  IElementType AND_EXPRESSION = new FuncElementType("AND_EXPRESSION");
  IElementType AND_OP_EXPRESSION = new FuncElementType("AND_OP_EXPRESSION");
  IElementType ASSIGNMENT_EXPRESSION = new FuncElementType("ASSIGNMENT_EXPRESSION");
  IElementType BLOCK = new FuncElementType("BLOCK");
  IElementType BOOLEAN_LITERAL = new FuncElementType("BOOLEAN_LITERAL");
  IElementType COMP_EXPRESSION = new FuncElementType("COMP_EXPRESSION");
  IElementType CONSTANT_VARIABLE_DECLARATION = new FuncElementType("CONSTANT_VARIABLE_DECLARATION");
  IElementType DECLARATION_ITEM = new FuncElementType("DECLARATION_ITEM");
  IElementType DECLARATION_LIST = new FuncElementType("DECLARATION_LIST");
  IElementType DO_UNTIL_STATEMENT = new FuncElementType("DO_UNTIL_STATEMENT");
  IElementType EQ_EXPRESSION = new FuncElementType("EQ_EXPRESSION");
  IElementType EXPRESSION = new FuncElementType("EXPRESSION");
  IElementType FUNCTION_CALL_ARGUMENTS = new FuncElementType("FUNCTION_CALL_ARGUMENTS");
  IElementType FUNCTION_CALL_EXPRESSION = new FuncElementType("FUNCTION_CALL_EXPRESSION");
  IElementType FUNCTION_DEFINITION = new FuncElementType("FUNCTION_DEFINITION");
  IElementType FUNCTION_SPECIFIERS = new FuncElementType("FUNCTION_SPECIFIERS");
  IElementType IF_NOT_STATEMENT = new FuncElementType("IF_NOT_STATEMENT");
  IElementType IF_STATEMENT = new FuncElementType("IF_STATEMENT");
  IElementType INDEX_ACCESS_EXPRESSION = new FuncElementType("INDEX_ACCESS_EXPRESSION");
  IElementType INLINE_ARRAY_EXPRESSION = new FuncElementType("INLINE_ARRAY_EXPRESSION");
  IElementType MEMBER_ACCESS_EXPRESSION = new FuncElementType("MEMBER_ACCESS_EXPRESSION");
  IElementType MODIFIER_ACCESS_EXPRESSION = new FuncElementType("MODIFIER_ACCESS_EXPRESSION");
  IElementType MULT_DIV_EXPRESSION = new FuncElementType("MULT_DIV_EXPRESSION");
  IElementType NUMBER_LITERAL = new FuncElementType("NUMBER_LITERAL");
  IElementType OR_EXPRESSION = new FuncElementType("OR_EXPRESSION");
  IElementType OR_OP_EXPRESSION = new FuncElementType("OR_OP_EXPRESSION");
  IElementType PARAMETER_DEF = new FuncElementType("PARAMETER_DEF");
  IElementType PARAMETER_LIST = new FuncElementType("PARAMETER_LIST");
  IElementType PLUS_MIN_EXPRESSION = new FuncElementType("PLUS_MIN_EXPRESSION");
  IElementType PRIMARY_EXPRESSION = new FuncElementType("PRIMARY_EXPRESSION");
  IElementType PRIMITIVE_TYPE_NAME = new FuncElementType("PRIMITIVE_TYPE_NAME");
  IElementType REPEAT_STATEMENT = new FuncElementType("REPEAT_STATEMENT");
  IElementType RETURN_DEF = new FuncElementType("RETURN_DEF");
  IElementType RETURN_ST = new FuncElementType("RETURN_ST");
  IElementType RETURN_TUPLE_STATEMENT = new FuncElementType("RETURN_TUPLE_STATEMENT");
  IElementType SEQ_EXPRESSION = new FuncElementType("SEQ_EXPRESSION");
  IElementType SHIFT_EXPRESSION = new FuncElementType("SHIFT_EXPRESSION");
  IElementType STATEMENT = new FuncElementType("STATEMENT");
  IElementType TERNARY_EXPRESSION = new FuncElementType("TERNARY_EXPRESSION");
  IElementType TYPED_DECLARATION_ITEM = new FuncElementType("TYPED_DECLARATION_ITEM");
  IElementType TYPED_DECLARATION_LIST = new FuncElementType("TYPED_DECLARATION_LIST");
  IElementType TYPED_ITEM = new FuncElementType("TYPED_ITEM");
  IElementType TYPED_LIST = new FuncElementType("TYPED_LIST");
  IElementType TYPE_NAME = new FuncElementType("TYPE_NAME");
  IElementType VARIABLE_DECLARATION = new FuncElementType("VARIABLE_DECLARATION");
  IElementType VARIABLE_DEFINITION = new FuncElementType("VARIABLE_DEFINITION");
  IElementType VAR_LITERAL = new FuncElementType("VAR_LITERAL");
  IElementType WHILE_STATEMENT = new FuncElementType("WHILE_STATEMENT");
  IElementType XOR_OP_EXPRESSION = new FuncElementType("XOR_OP_EXPRESSION");

  IElementType AND = new FuncTokenType("&");
  IElementType ANDAND = new FuncTokenType("&&");
  IElementType AND_ASSIGN = new FuncTokenType("&=");
  IElementType ASSIGN = new FuncTokenType("=");
  IElementType BOOLEANLITERAL = new FuncTokenType("booleanLiteral");
  IElementType BUILDER = new FuncTokenType("builder");
  IElementType CARET = new FuncTokenType("^");
  IElementType CELL = new FuncTokenType("cell");
  IElementType COLON = new FuncTokenType(":");
  IElementType COMMA = new FuncTokenType(",");
  IElementType COMMENT = new FuncTokenType("comment");
  IElementType CONT = new FuncTokenType("cont");
  IElementType DEC = new FuncTokenType("--");
  IElementType DECIMALNUMBER = new FuncTokenType("decimalNumber");
  IElementType DIV = new FuncTokenType("/");
  IElementType DIV_ASSIGN = new FuncTokenType("/=");
  IElementType DO = new FuncTokenType("do");
  IElementType DOT = new FuncTokenType(".");
  IElementType ELSE = new FuncTokenType("else");
  IElementType EQ = new FuncTokenType("==");
  IElementType EXPONENT = new FuncTokenType("**");
  IElementType FIXEDNUMBER = new FuncTokenType("fixedNumber");
  IElementType HEXLITERAL = new FuncTokenType("hexLiteral");
  IElementType HEXNUMBER = new FuncTokenType("hexNumber");
  IElementType IDENTIFIER = new FuncTokenType("Identifier");
  IElementType IF = new FuncTokenType("if");
  IElementType IFNOT = new FuncTokenType("ifnot");
  IElementType IMPURE = new FuncTokenType("impure");
  IElementType INC = new FuncTokenType("++");
  IElementType INLINE = new FuncTokenType("inline");
  IElementType INLINE_REF = new FuncTokenType("inline_ref");
  IElementType INT = new FuncTokenType("int");
  IElementType LBRACE = new FuncTokenType("{");
  IElementType LBRACKET = new FuncTokenType("[");
  IElementType LEFT_ASSEMBLY = new FuncTokenType(":=");
  IElementType LESS = new FuncTokenType("<");
  IElementType LESSEQ = new FuncTokenType("<=");
  IElementType LPAREN = new FuncTokenType("(");
  IElementType LSHIFT = new FuncTokenType("<<");
  IElementType LSHIFT_ASSIGN = new FuncTokenType("<<=");
  IElementType METHOD_ID = new FuncTokenType("method_id");
  IElementType MINUS = new FuncTokenType("-");
  IElementType MINUS_ASSIGN = new FuncTokenType("-=");
  IElementType MORE = new FuncTokenType(">");
  IElementType MOREEQ = new FuncTokenType(">=");
  IElementType MULT = new FuncTokenType("*");
  IElementType MULT_ASSIGN = new FuncTokenType("*=");
  IElementType NAT_SPEC_TAG = new FuncTokenType("NAT_SPEC_TAG");
  IElementType NEQ = new FuncTokenType("!=");
  IElementType NOT = new FuncTokenType("!");
  IElementType OR = new FuncTokenType("|");
  IElementType OROR = new FuncTokenType("||");
  IElementType OR_ASSIGN = new FuncTokenType("|=");
  IElementType PERCENT = new FuncTokenType("%");
  IElementType PERCENT_ASSIGN = new FuncTokenType("%=");
  IElementType PLUS = new FuncTokenType("+");
  IElementType PLUS_ASSIGN = new FuncTokenType("+=");
  IElementType QUESTION = new FuncTokenType("?");
  IElementType RBRACE = new FuncTokenType("}");
  IElementType RBRACKET = new FuncTokenType("]");
  IElementType REPEAT = new FuncTokenType("repeat");
  IElementType RETURN = new FuncTokenType("return");
  IElementType RIGHT_ASSEMBLY = new FuncTokenType("=:");
  IElementType RPAREN = new FuncTokenType(")");
  IElementType RSHIFT = new FuncTokenType(">>");
  IElementType RSHIFT_ASSIGN = new FuncTokenType(">>=");
  IElementType SCIENTIFICNUMBER = new FuncTokenType("scientificNumber");
  IElementType SEMICOLON = new FuncTokenType(";");
  IElementType SLICE = new FuncTokenType("slice");
  IElementType STRINGLITERAL = new FuncTokenType("stringLiteral");
  IElementType TILDE = new FuncTokenType("~");
  IElementType TO = new FuncTokenType("=>");
  IElementType TUPLE = new FuncTokenType("tuple");
  IElementType UNTIL = new FuncTokenType("until");
  IElementType VAR = new FuncTokenType("var");
  IElementType WHILE = new FuncTokenType("while");
  IElementType XOR_ASSIGN = new FuncTokenType("^=");

  class Factory {
    public static PsiElement createElement(ASTNode node) {
      IElementType type = node.getElementType();
      if (type == AND_EXPRESSION) {
        return new FuncAndExpressionImpl(node);
      }
      else if (type == AND_OP_EXPRESSION) {
        return new FuncAndOpExpressionImpl(node);
      }
      else if (type == ASSIGNMENT_EXPRESSION) {
        return new FuncAssignmentExpressionImpl(node);
      }
      else if (type == BLOCK) {
        return new FuncBlockImpl(node);
      }
      else if (type == BOOLEAN_LITERAL) {
        return new FuncBooleanLiteralImpl(node);
      }
      else if (type == COMP_EXPRESSION) {
        return new FuncCompExpressionImpl(node);
      }
      else if (type == CONSTANT_VARIABLE_DECLARATION) {
        return new FuncConstantVariableDeclarationImpl(node);
      }
      else if (type == DECLARATION_ITEM) {
        return new FuncDeclarationItemImpl(node);
      }
      else if (type == DECLARATION_LIST) {
        return new FuncDeclarationListImpl(node);
      }
      else if (type == DO_UNTIL_STATEMENT) {
        return new FuncDoUntilStatementImpl(node);
      }
      else if (type == EQ_EXPRESSION) {
        return new FuncEqExpressionImpl(node);
      }
      else if (type == FUNCTION_CALL_ARGUMENTS) {
        return new FuncFunctionCallArgumentsImpl(node);
      }
      else if (type == FUNCTION_CALL_EXPRESSION) {
        return new FuncFunctionCallExpressionImpl(node);
      }
      else if (type == FUNCTION_DEFINITION) {
        return new FuncFunctionDefinitionImpl(node);
      }
      else if (type == FUNCTION_SPECIFIERS) {
        return new FuncFunctionSpecifiersImpl(node);
      }
      else if (type == IF_NOT_STATEMENT) {
        return new FuncIfNotStatementImpl(node);
      }
      else if (type == IF_STATEMENT) {
        return new FuncIfStatementImpl(node);
      }
      else if (type == INDEX_ACCESS_EXPRESSION) {
        return new FuncIndexAccessExpressionImpl(node);
      }
      else if (type == INLINE_ARRAY_EXPRESSION) {
        return new FuncInlineArrayExpressionImpl(node);
      }
      else if (type == MEMBER_ACCESS_EXPRESSION) {
        return new FuncMemberAccessExpressionImpl(node);
      }
      else if (type == MODIFIER_ACCESS_EXPRESSION) {
        return new FuncModifierAccessExpressionImpl(node);
      }
      else if (type == MULT_DIV_EXPRESSION) {
        return new FuncMultDivExpressionImpl(node);
      }
      else if (type == NUMBER_LITERAL) {
        return new FuncNumberLiteralImpl(node);
      }
      else if (type == OR_EXPRESSION) {
        return new FuncOrExpressionImpl(node);
      }
      else if (type == OR_OP_EXPRESSION) {
        return new FuncOrOpExpressionImpl(node);
      }
      else if (type == PARAMETER_DEF) {
        return new FuncParameterDefImpl(node);
      }
      else if (type == PARAMETER_LIST) {
        return new FuncParameterListImpl(node);
      }
      else if (type == PLUS_MIN_EXPRESSION) {
        return new FuncPlusMinExpressionImpl(node);
      }
      else if (type == PRIMARY_EXPRESSION) {
        return new FuncPrimaryExpressionImpl(node);
      }
      else if (type == PRIMITIVE_TYPE_NAME) {
        return new FuncPrimitiveTypeNameImpl(node);
      }
      else if (type == REPEAT_STATEMENT) {
        return new FuncRepeatStatementImpl(node);
      }
      else if (type == RETURN_DEF) {
        return new FuncReturnDefImpl(node);
      }
      else if (type == RETURN_ST) {
        return new FuncReturnStImpl(node);
      }
      else if (type == RETURN_TUPLE_STATEMENT) {
        return new FuncReturnTupleStatementImpl(node);
      }
      else if (type == SEQ_EXPRESSION) {
        return new FuncSeqExpressionImpl(node);
      }
      else if (type == SHIFT_EXPRESSION) {
        return new FuncShiftExpressionImpl(node);
      }
      else if (type == STATEMENT) {
        return new FuncStatementImpl(node);
      }
      else if (type == TERNARY_EXPRESSION) {
        return new FuncTernaryExpressionImpl(node);
      }
      else if (type == TYPED_DECLARATION_ITEM) {
        return new FuncTypedDeclarationItemImpl(node);
      }
      else if (type == TYPED_DECLARATION_LIST) {
        return new FuncTypedDeclarationListImpl(node);
      }
      else if (type == TYPED_ITEM) {
        return new FuncTypedItemImpl(node);
      }
      else if (type == TYPED_LIST) {
        return new FuncTypedListImpl(node);
      }
      else if (type == VARIABLE_DECLARATION) {
        return new FuncVariableDeclarationImpl(node);
      }
      else if (type == VARIABLE_DEFINITION) {
        return new FuncVariableDefinitionImpl(node);
      }
      else if (type == VAR_LITERAL) {
        return new FuncVarLiteralImpl(node);
      }
      else if (type == WHILE_STATEMENT) {
        return new FuncWhileStatementImpl(node);
      }
      else if (type == XOR_OP_EXPRESSION) {
        return new FuncXorOpExpressionImpl(node);
      }
      throw new AssertionError("Unknown element type: " + type);
    }
  }
}
