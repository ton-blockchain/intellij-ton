package org.ton.intellij.tolk.toolchain

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.resolveFromRootOrRelative
import org.ton.intellij.tolk.toolchain.flavor.TolkToolchainFlavor
import java.nio.file.Path
import kotlin.io.path.absolutePathString

interface TolkToolchain {
    val version: String

    val stdlibDir: VirtualFile?

    val rootDir: VirtualFile?

    val homePath: String

    companion object {
        fun fromPath(homePath: Path): TolkToolchain = fromPath(homePath.absolutePathString())
        fun fromPath(homePath: String): TolkToolchain {
            if (homePath.isBlank()) {
                return NULL
            }

            val virtualFileManager = VirtualFileManager.getInstance()
            val rootDir = virtualFileManager.findFileByNioPath(Path.of(homePath)) ?: return NULL
            return fromDirectory(rootDir)
        }

        fun fromDirectory(rootDir: VirtualFile): TolkToolchain {
            val version = TolkConfigurationUtil.guessToolchainVersion(rootDir.path)
            return TolkLocalToolchain(version, rootDir)
        }

        fun suggest(project: Project): TolkToolchain? {
            return TolkToolchainFlavor.getApplicableFlavors()
                .asSequence()
                .flatMap { it.suggestHomePaths(project) }
                .map { fromPath(it) }
                .firstOrNull()
        }

        val NULL = object : TolkToolchain {
            override val version: String get() = ""
            override val stdlibDir: VirtualFile? get() = null
            override val rootDir: VirtualFile? get() = null
            override val homePath: String get() = ""
        }
    }
}

class TolkLocalToolchain(
    override val version: String,
    override val rootDir: VirtualFile,
) : TolkToolchain {
    private val jsLibDir by lazy {
        rootDir.resolveFromRootOrRelative(TolkConfigurationUtil.STANDARD_JS_STDLIB_PATH)
    }

    override val stdlibDir: VirtualFile?
        get() {
            if (homePath.contains("node_modules")) {
                return jsLibDir
            }
            return rootDir.resolveFromRootOrRelative(TolkConfigurationUtil.COMPILER_REPO_STANDARD_LIB_PATH)
        }

    override val homePath: String get() = rootDir.path

    override fun toString(): String {
        return "TolkLocalToolchain(version='$version', homePath='$homePath')"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TolkLocalToolchain

        return FileUtil.comparePaths(homePath, other.homePath) == 0
    }

    override fun hashCode(): Int = homePath.hashCode()
}
