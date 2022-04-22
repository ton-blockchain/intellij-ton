package org.ton.intellij

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil

fun loadTextResource(ctx: Any, resource: String): String =
    ctx.javaClass.classLoader.getResourceAsStream(resource)?.bufferedReader()?.use {
        it.readText()
    } ?: ""

val Project.psiManager get() = PsiManager.getInstance(this)

inline fun <reified T : PsiElement> PsiElement.childOfType(strict: Boolean = true): T? =
    childOfType(T::class.java, strict)

fun <T : PsiElement> PsiElement.childOfType(type: Class<T>, strict: Boolean = true): T? =
    PsiTreeUtil.findChildOfType(this, type, strict)

inline fun <reified T : PsiElement> PsiElement.parentOfType(strict: Boolean = true, minStartOffset: Int = -1): T? =
    PsiTreeUtil.getParentOfType(this, T::class.java, strict, minStartOffset)

inline fun <reified T : PsiElement> PsiElement.collectElements() =
    PsiTreeUtil.collectElementsOfType(this, T::class.java)