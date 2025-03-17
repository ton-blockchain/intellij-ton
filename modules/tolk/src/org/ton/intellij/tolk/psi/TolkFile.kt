package org.ton.intellij.tolk.psi

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.findPsiFile
import com.intellij.psi.FileViewProvider
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.ton.intellij.tolk.TolkFileType
import org.ton.intellij.tolk.TolkLanguage
import org.ton.intellij.tolk.psi.impl.resolveFile
import org.ton.intellij.tolk.sdk.TolkSdkManager
import org.ton.intellij.tolk.stub.TolkFileStub
import org.ton.intellij.tolk.stub.type.TolkConstVarStubElementType
import org.ton.intellij.tolk.stub.type.TolkFunctionStubElementType
import org.ton.intellij.tolk.stub.type.TolkGlobalVarStubElementType
import org.ton.intellij.tolk.stub.type.TolkIncludeDefinitionStubElementType
import org.ton.intellij.util.getChildrenByType
import org.ton.intellij.util.recursionGuard

//private fun processFile(context: TolkElement, file: TolkFile) {
//    recursionGuard(file, {
//        for (includeDefinition in file.includeDefinitions) {
//            val nextFile = includeDefinition.reference?.resolve()
//            if (nextFile !is TolkFile) continue
//            processFile(context, nextFile)
//        }

class TolkFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, TolkLanguage), TolkElement, TolkInferenceContextOwner {
    override fun getFileType(): FileType = TolkFileType

    override fun getStub(): TolkFileStub? = super.getStub() as? TolkFileStub

    fun collectIncludedFiles(includeSelf: Boolean = true): Set<TolkFile> {
        val sdk = TolkSdkManager[project].getSdkRef().resolve(project)?.library?.sourceRoots?.mapNotNull {
            it.findPsiFile(project) as? TolkFile
        }?.toMutableSet() ?: mutableSetOf()
        sdk.remove(this)
        return collectIncludedFiles(sdk, includeSelf)
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

    fun import(file: TolkFile) {
        if (file == this) return
        var path = VfsUtil.findRelativePath(virtualFile ?: return, file.virtualFile ?: return, '/') ?: return
        val needImport = includeDefinitions.none { it.resolveFile(it.project) == file.virtualFile }
        if (!needImport) return

        val factory = TolkPsiFactory[project]
        val sdk = TolkSdkManager[project].getSdkRef().resolve(project)
        if (sdk != null && VfsUtil.isAncestor(sdk.stdlibFile, file.virtualFile, false)) {
            val sdkPath = sdk.stdlibFile.path
            path = file.virtualFile.path.replace(sdkPath, "@stdlib")
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
