package org.ton.intellij.tolk.psi

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.findFile
import com.intellij.openapi.vfs.findPsiFile
import com.intellij.psi.FileViewProvider
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.ton.intellij.tolk.TolkFileType
import org.ton.intellij.tolk.TolkLanguage
import org.ton.intellij.tolk.ide.configurable.tolkSettings
import org.ton.intellij.tolk.psi.impl.resolveFile
import org.ton.intellij.tolk.stub.TolkFileStub
import org.ton.intellij.tolk.stub.type.*
import org.ton.intellij.util.getChildrenByType
import org.ton.intellij.util.recursionGuard

//private fun processFile(context: TolkElement, file: TolkFile) {
//    recursionGuard(file, {
//        for (includeDefinition in file.includeDefinitions) {
//            val nextFile = includeDefinition.reference?.resolve()
//            if (nextFile !is TolkFile) continue
//            processFile(context, nextFile)
//        }

class TolkFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, TolkLanguage), TolkElement,
    TolkInferenceContextOwner {
    override fun getFileType(): FileType = TolkFileType

    override fun getStub(): TolkFileStub? = super.getStub() as? TolkFileStub

    fun collectIncludedFiles(includeSelf: Boolean = true): Set<TolkFile> {
        val files = mutableSetOf<TolkFile>()
        val stdlibDir = project.tolkSettings.toolchain?.stdlibDir
        if (stdlibDir != null) {
            val commonStdlib = stdlibDir.findFile("common.tolk")?.findPsiFile(project)
            if (commonStdlib is TolkFile) {
                files.add(commonStdlib)
            }
        }
        return collectIncludedFiles(files, includeSelf)
    }

    private fun collectIncludedFiles(collection: MutableSet<TolkFile>, includeSelf: Boolean): MutableSet<TolkFile> {
        recursionGuard(this, false) {
            for (includeDefinition in includeDefinitions) {
//                val nextFile = includeDefinition.reference?.resolve()
                val nextFile = includeDefinition.resolveFile(project)?.findPsiFile(project)
                if (nextFile !is TolkFile) continue
                collection.add(nextFile)
            }
            if (includeSelf) {
                collection.add(this)
            }
        }
        return collection
    }

    val includeDefinitions: List<TolkIncludeDefinition>
        get() = CachedValuesManager.getCachedValue(this) {
            val stub = stub
            val children = if (stub != null) getChildrenByType(
                stub,
                TolkElementTypes.INCLUDE_DEFINITION,
                TolkIncludeDefinitionStubElementType.ARRAY_FACTORY
            ) else {
                findChildrenByClass(TolkIncludeDefinition::class.java).toList()
            }
            CachedValueProvider.Result.create(children, this)
        }

    val functions: List<TolkFunction>
        get() = CachedValuesManager.getCachedValue(this) {
            val stub = stub
            val functions = if (stub != null) {
                getChildrenByType(stub, TolkElementTypes.FUNCTION, TolkFunctionStubElementType.ARRAY_FACTORY)
            } else {
                findChildrenByClass(TolkFunction::class.java).toList()
            }
            CachedValueProvider.Result.create(functions, this)
        }

    val constVars: List<TolkConstVar>
        get() = CachedValuesManager.getCachedValue(this) {
            val stub = stub
            val constVars = if (stub != null) {
                getChildrenByType(stub, TolkElementTypes.CONST_VAR, TolkConstVarStubElementType.ARRAY_FACTORY)
            } else {
                findChildrenByClass(TolkConstVar::class.java).toList()
            }
            CachedValueProvider.Result.create(constVars, this)
        }

    val globalVars: List<TolkGlobalVar>
        get() = CachedValuesManager.getCachedValue(this) {
            val stub = stub
            val constVars = if (stub != null) {
                getChildrenByType(stub, TolkElementTypes.GLOBAL_VAR, TolkGlobalVarStubElementType.ARRAY_FACTORY)
            } else {
                findChildrenByClass(TolkGlobalVar::class.java).toList()
            }
            CachedValueProvider.Result.create(constVars, this)
        }

    val typeDefs: List<TolkTypeDef>
        get() = CachedValuesManager.getCachedValue(this) {
            val stub = stub
            val typeDefs = if (stub != null) {
                getChildrenByType(stub, TolkElementTypes.TYPE_DEF, TolkTypeDefStubElementType.ARRAY_FACTORY)
            } else {
                findChildrenByClass(TolkTypeDef::class.java).toList()
            }
            CachedValueProvider.Result.create(typeDefs, this)
        }

    val structs: List<TolkStruct>
        get() = CachedValuesManager.getCachedValue(this) {
            val stub = stub
            val typeDefs = if (stub != null) {
                getChildrenByType(stub, TolkElementTypes.STRUCT, TolkStructStubElementType.ARRAY_FACTORY)
            } else {
                findChildrenByClass(TolkStruct::class.java).toList()
            }
            CachedValueProvider.Result.create(typeDefs, this)
        }


    fun import(file: TolkFile) {
        if (file == this) return
        if (!file.isPhysical) return
        var path = VfsUtil.findRelativePath(virtualFile ?: return, file.virtualFile ?: return, '/') ?: return
        val needImport = includeDefinitions.none { it.resolveFile(it.project) == file.virtualFile }
        if (!needImport) return

        val factory = TolkPsiFactory[project]
        val sdk = project.tolkSettings.toolchain?.stdlibDir
        if (sdk != null && VfsUtil.isAncestor(sdk, file.virtualFile, false)) {
            val sdkPath = sdk.path
            path = file.virtualFile.path.replace(sdkPath, "@stdlib")
            if (path == "@stdlib/common.tolk") {
                return
            }
        }
        val newInclude = factory.createIncludeDefinition(path)

        tryIncludeAtCorrectLocation(newInclude)
    }

    private fun tryIncludeAtCorrectLocation(includeDefinition: TolkIncludeDefinition) {
        val newline = TolkPsiFactory[project].createNewline()
        val includes = includeDefinitions
        if (includes.isEmpty()) {
            addBefore(includeDefinition, firstChild)
            addAfter(newline, firstChild)
            return
        }

        val (less, greater) = includes.partition { INCLUDE_COMPARE.compare(it, includeDefinition) < 0 }
        val anchorBefore = less.lastOrNull()
        val anchorAfter = greater.firstOrNull()
        when {
            anchorBefore != null -> {
                val addedItem = addAfter(includeDefinition, anchorBefore)
                addBefore(newline, addedItem)
            }

            anchorAfter != null -> {
                val addedItem = addBefore(includeDefinition, anchorAfter)
                addAfter(newline, addedItem)
            }

            else -> error("unreachable")
        }
    }

    override fun toString(): String = "TolkFile($name)"
}

private val INCLUDE_COMPARE: Comparator<TolkIncludeDefinition> =
    compareBy(
        {
            if (it.stringLiteral?.rawString?.text?.startsWith("@stdlib") == true) {
                0
            } else {
                1
            }
        },
        {
            it.stringLiteral?.rawString?.text?.count { c -> c == '/' }
        },
        {
            it.stringLiteral?.rawString?.text?.lowercase()
        }
    )
