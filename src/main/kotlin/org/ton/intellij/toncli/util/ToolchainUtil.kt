package org.ton.intellij.toncli.util

import com.intellij.openapi.util.SystemInfo
import com.intellij.util.text.SemVer
import java.nio.file.Files
import java.nio.file.Path

fun String.parseSemVer(): SemVer = checkNotNull(SemVer.parseFromText(this)) { "Invalid version value: $this" }

fun Path.hasExecutable(toolName: String): Boolean = pathToExecutable(toolName).isExecutable()

fun Path.pathToExecutable(toolName: String): Path {
    val exeName = if (SystemInfo.isWindows) "$toolName.exe" else toolName
    return resolve(exeName).toAbsolutePath()
}

fun Path.isExecutable(): Boolean = Files.isExecutable(this)