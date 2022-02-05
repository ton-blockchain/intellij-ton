package com.github.andreypfau.intellijton.func.completion

import com.github.andreypfau.intellijton.func.psi.FuncBlock
import com.github.andreypfau.intellijton.func.psi.FuncFunctionCallArguments
import com.github.andreypfau.intellijton.func.psi.FuncFunctionCallExpression
import com.github.andreypfau.intellijton.func.psi.FuncTypes
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.StandardPatterns
import com.intellij.psi.PsiElement

fun expression(): ElementPattern<PsiElement> =
    StandardPatterns.or(
        functionCall(), block(), functionCallArguments()
    )

fun functionCall(): ElementPattern<PsiElement> =
    PlatformPatterns.psiElement(FuncTypes.IDENTIFIER).inside(FuncFunctionCallExpression::class.java)

fun block(): ElementPattern<PsiElement> =
    PlatformPatterns.psiElement(FuncTypes.IDENTIFIER).inside(FuncBlock::class.java)

fun functionCallArguments(): ElementPattern<PsiElement> =
    PlatformPatterns.psiElement(FuncTypes.IDENTIFIER).inside(FuncFunctionCallArguments::class.java)