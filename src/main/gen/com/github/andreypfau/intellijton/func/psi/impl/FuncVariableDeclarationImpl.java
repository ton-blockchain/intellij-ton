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

public class FuncVariableDeclarationImpl extends ASTWrapperPsiElement implements FuncVariableDeclaration {

  public FuncVariableDeclarationImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull FuncVisitor visitor) {
    visitor.visitVariableDeclaration(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof FuncVisitor) accept((FuncVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public FuncDeclarationList getDeclarationList() {
    return findChildByClass(FuncDeclarationList.class);
  }

  @Override
  @Nullable
  public FuncTypeName getTypeName() {
    return findChildByClass(FuncTypeName.class);
  }

  @Override
  @Nullable
  public FuncTypedDeclarationList getTypedDeclarationList() {
    return findChildByClass(FuncTypedDeclarationList.class);
  }

  @Override
  @Nullable
  public FuncTypedList getTypedList() {
    return findChildByClass(FuncTypedList.class);
  }

  @Override
  @Nullable
  public PsiElement getIdentifier() {
    return findChildByType(IDENTIFIER);
  }

}
