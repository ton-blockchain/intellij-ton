package org.ton.intellij

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.Experiments
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.streams.asSequence

private val LOG = Logger.getInstance("#org.ton.intellij.utils")

val isUnitTestMode: Boolean get() = ApplicationManager.getApplication().isUnitTestMode
val isDispatchThread: Boolean get() = ApplicationManager.getApplication().isDispatchThread
val isHeadlessEnvironment: Boolean get() = ApplicationManager.getApplication().isHeadlessEnvironment

fun <T> Project.computeWithCancelableProgress(
        @Suppress("UnstableApiUsage") @NlsContexts.ProgressTitle title: String,
        supplier: () -> T
): T {
    if (isUnitTestMode) {
        return supplier()
    }
    return ProgressManager.getInstance().runProcessWithProgressSynchronously<T, Exception>(supplier, title, true, this)
}

fun String.toPath(): Path = Paths.get(this)
fun String.toPathOrNull(): Path? = pathOrNull(this::toPath)
fun Path.resolveOrNull(other: String): Path? = pathOrNull { resolve(other) }
fun Path.list(): Sequence<Path> = Files.list(this).asSequence()

private inline fun pathOrNull(block: () -> Path): Path? {
    return try {
        block()
    } catch (e: InvalidPathException) {
        LOG.warn(e)
        null
    }
}

val VirtualFile.pathAsPath: Path get() = Paths.get(path)

fun isFeatureEnabled(featureId: String): Boolean {
    // Hack to pass values of experimental features in headless IDE run
    // Should help to configure IDE-based tools like Qodana
    if (isHeadlessEnvironment) {
        val value = System.getProperty(featureId)?.toBooleanStrictOrNull()
        if (value != null) return value
    }

    return Experiments.getInstance().isFeatureEnabled(featureId)
}

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

fun PsiElement.processElements(processor: (PsiElement)->Boolean) = PsiTreeUtil.processElements(this, processor)