// This is a generated file. Not intended for manual editing.
package com.github.andreypfau.intellijton.func.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static com.github.andreypfau.intellijton.func.psi.FuncTypes.*;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.github.andreypfau.intellijton.func.psi.*;

public class FuncNumberLiteralImpl extends ASTWrapperPsiElement implements FuncNumberLiteral {

  public FuncNumberLiteralImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull FuncVisitor visitor) {
    visitor.visitNumberLiteral(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof FuncVisitor) accept((FuncVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public PsiElement getDecimalNumber() {
    return findChildByType(DECIMALNUMBER);
  }

  @Override
  @Nullable
  public PsiElement getFixedNumber() {
    return findChildByType(FIXEDNUMBER);
  }

  @Override
  @Nullable
  public PsiElement getHexNumber() {
    return findChildByType(HEXNUMBER);
  }

  @Override
  @Nullable
  public PsiElement getScientificNumber() {
    return findChildByType(SCIENTIFICNUMBER);
  }

}
