// This is a generated file. Not intended for manual editing.
package com.github.andreypfau.intellijton.func.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static com.github.andreypfau.intellijton.func.psi.FuncTypes.*;
import com.github.andreypfau.intellijton.func.psi.*;

public class FuncPrimaryExpressionImpl extends FuncExpressionImpl implements FuncPrimaryExpression {

  public FuncPrimaryExpressionImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public void accept(@NotNull FuncVisitor visitor) {
    visitor.visitPrimaryExpression(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof FuncVisitor) accept((FuncVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public FuncBooleanLiteral getBooleanLiteral() {
    return findChildByClass(FuncBooleanLiteral.class);
  }

  @Override
  @Nullable
  public FuncNumberLiteral getNumberLiteral() {
    return findChildByClass(FuncNumberLiteral.class);
  }

  @Override
  @Nullable
  public FuncPrimitiveTypeName getPrimitiveTypeName() {
    return findChildByClass(FuncPrimitiveTypeName.class);
  }

  @Override
  @Nullable
  public FuncVarLiteral getVarLiteral() {
    return findChildByClass(FuncVarLiteral.class);
  }

  @Override
  @Nullable
  public PsiElement getStringLiteral() {
    return findChildByType(STRINGLITERAL);
  }

}
