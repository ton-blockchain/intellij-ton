package com.github.andreypfau.intellijton.func.completion

import com.github.andreypfau.intellijton.func.psi.FuncBlockStatement
import com.github.andreypfau.intellijton.func.psi.FuncExpressionStatement
import com.github.andreypfau.intellijton.func.psi.FuncFunctionApplication
import com.github.andreypfau.intellijton.func.psi.FuncTypes
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.StandardPatterns
import com.intellij.psi.PsiElement

//fun expression(): ElementPattern<PsiElement> =
//    StandardPatterns.or(
//        functionCall(), functionCallArguments()
//    )
//
//fun functionCall(): ElementPattern<PsiElement> =
//    PlatformPatterns.psiElement(FuncTypes.IDENTIFIER).inside(FuncFunctionCallExpression::class.java)
//
fun aaa() = StandardPatterns.or(
//    PlatformPatterns.psiElement(FuncTypes.IDENTIFIER).inside(FuncBlockStatement::class.java),
//    PlatformPatterns.psiElement(FuncTypes.IDENTIFIER).inside(FuncFunctionApplication::class.java),
//    PlatformPatterns.psiElement(FuncTypes.FUNCTION_NAME).inside(FuncBlockStatement::class.java),
//    PlatformPatterns.psiElement(FuncTypes.FUNCTION_NAME).inside(FuncFunctionApplication::class.java),
//
//    PlatformPatterns.psiElement(FuncTypes.FUNCTION_NAME).inside(FuncExpressionStatement::class.java),
//    PlatformPatterns.psiElement(FuncTypes.IDENTIFIER).inside(FuncExpressionStatement::class.java),
    PlatformPatterns.psiElement()
)


fun functionApplication()=
    PlatformPatterns.psiElement(FuncTypes.IDENTIFIER).inside(FuncBlockStatement::class.java)
//
//fun functionCallArguments(): ElementPattern<PsiElement> =
//    PlatformPatterns.psiElement(FuncTypes.IDENTIFIER).inside(FuncFunctionCallArguments::class.java)