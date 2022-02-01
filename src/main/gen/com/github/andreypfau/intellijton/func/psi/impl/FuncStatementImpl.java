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

public class FuncStatementImpl extends ASTWrapperPsiElement implements FuncStatement {

  public FuncStatementImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull FuncVisitor visitor) {
    visitor.visitStatement(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof FuncVisitor) accept((FuncVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public FuncBlock getBlock() {
    return findChildByClass(FuncBlock.class);
  }

  @Override
  @Nullable
  public FuncDoUntilStatement getDoUntilStatement() {
    return findChildByClass(FuncDoUntilStatement.class);
  }

  @Override
  @Nullable
  public FuncExpression getExpression() {
    return findChildByClass(FuncExpression.class);
  }

  @Override
  @Nullable
  public FuncIfNotStatement getIfNotStatement() {
    return findChildByClass(FuncIfNotStatement.class);
  }

  @Override
  @Nullable
  public FuncIfStatement getIfStatement() {
    return findChildByClass(FuncIfStatement.class);
  }

  @Override
  @Nullable
  public FuncRepeatStatement getRepeatStatement() {
    return findChildByClass(FuncRepeatStatement.class);
  }

  @Override
  @Nullable
  public FuncReturnSt getReturnSt() {
    return findChildByClass(FuncReturnSt.class);
  }

  @Override
  @Nullable
  public FuncReturnTupleStatement getReturnTupleStatement() {
    return findChildByClass(FuncReturnTupleStatement.class);
  }

  @Override
  @Nullable
  public FuncVariableDefinition getVariableDefinition() {
    return findChildByClass(FuncVariableDefinition.class);
  }

  @Override
  @Nullable
  public FuncWhileStatement getWhileStatement() {
    return findChildByClass(FuncWhileStatement.class);
  }

}
