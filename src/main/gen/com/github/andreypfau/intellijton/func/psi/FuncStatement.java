// This is a generated file. Not intended for manual editing.
package com.github.andreypfau.intellijton.func.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface FuncStatement extends PsiElement {

  @Nullable
  FuncBlock getBlock();

  @Nullable
  FuncDoUntilStatement getDoUntilStatement();

  @Nullable
  FuncExpression getExpression();

  @Nullable
  FuncIfNotStatement getIfNotStatement();

  @Nullable
  FuncIfStatement getIfStatement();

  @Nullable
  FuncRepeatStatement getRepeatStatement();

  @Nullable
  FuncReturnSt getReturnSt();

  @Nullable
  FuncReturnTupleStatement getReturnTupleStatement();

  @Nullable
  FuncVariableDefinition getVariableDefinition();

  @Nullable
  FuncWhileStatement getWhileStatement();

}
