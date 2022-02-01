// This is a generated file. Not intended for manual editing.
package com.github.andreypfau.intellijton.func.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface FuncFunctionDefinition extends PsiElement {

  @Nullable
  FuncBlock getBlock();

  @NotNull
  List<FuncFunctionSpecifiers> getFunctionSpecifiersList();

  @Nullable
  FuncParameterList getParameterList();

  @NotNull
  FuncReturnDef getReturnDef();

  @NotNull
  PsiElement getIdentifier();

}
