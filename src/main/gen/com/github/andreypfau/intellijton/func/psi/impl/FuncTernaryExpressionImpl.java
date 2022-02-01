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

public class FuncTernaryExpressionImpl extends FuncExpressionImpl implements FuncTernaryExpression {

  public FuncTernaryExpressionImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public void accept(@NotNull FuncVisitor visitor) {
    visitor.visitTernaryExpression(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof FuncVisitor) accept((FuncVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public List<FuncExpression> getExpressionList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, FuncExpression.class);
  }

}
