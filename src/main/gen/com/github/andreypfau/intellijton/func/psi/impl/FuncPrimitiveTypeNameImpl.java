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

public class FuncPrimitiveTypeNameImpl extends FuncTypeNameImpl implements FuncPrimitiveTypeName {

  public FuncPrimitiveTypeNameImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public void accept(@NotNull FuncVisitor visitor) {
    visitor.visitPrimitiveTypeName(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof FuncVisitor) accept((FuncVisitor)visitor);
    else super.accept(visitor);
  }

}
