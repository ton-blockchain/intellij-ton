// This is a generated file. Not intended for manual editing.
package com.github.andreypfau.intellijton.func.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface FuncPrimaryExpression extends FuncExpression {

  @Nullable
  FuncBooleanLiteral getBooleanLiteral();

  @Nullable
  FuncNumberLiteral getNumberLiteral();

  @Nullable
  FuncPrimitiveTypeName getPrimitiveTypeName();

  @Nullable
  FuncVarLiteral getVarLiteral();

  @Nullable
  PsiElement getStringLiteral();

}
