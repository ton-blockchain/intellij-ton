package com.github.andreypfau.intellijton.func.completion

//fun expression(): ElementPattern<PsiElement> =
//    StandardPatterns.or(
//        functionCall(), functionCallArguments()
//    )
//
//fun functionCall(): ElementPattern<PsiElement> =
//    PlatformPatterns.psiElement(FuncTypes.IDENTIFIER).inside(FuncFunctionCallExpression::class.java)
//
//fun block(): ElementPattern<PsiElement> =
//    PlatformPatterns.psiElement(FuncTypes.IDENTIFIER).inside(FuncBlock::class.java)
//
//fun functionCallArguments(): ElementPattern<PsiElement> =
//    PlatformPatterns.psiElement(FuncTypes.IDENTIFIER).inside(FuncFunctionCallArguments::class.java)