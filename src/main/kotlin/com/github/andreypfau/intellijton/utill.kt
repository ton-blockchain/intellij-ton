package com.github.andreypfau.intellijton

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil

fun loadTextResource(ctx: Any, resource: String): String =
    ctx.javaClass.classLoader.getResourceAsStream(resource)!!.bufferedReader().use {
        it.readText()
    }

inline fun <reified T : PsiElement> PsiElement.childOfType(strict: Boolean = true): T? =
    PsiTreeUtil.findChildOfType(this, T::class.java, strict)

inline fun <reified T : PsiElement> PsiElement.parentOfType(strict: Boolean = true, minStartOffset: Int = -1): T? =
    PsiTreeUtil.getParentOfType(this, T::class.java, strict, minStartOffset)

