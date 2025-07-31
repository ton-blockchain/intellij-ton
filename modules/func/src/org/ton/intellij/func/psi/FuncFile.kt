package org.ton.intellij.func.psi

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.FileViewProvider
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.ton.intellij.func.FuncFileType
import org.ton.intellij.func.FuncLanguage
import org.ton.intellij.func.stub.FuncFileStub
import org.ton.intellij.func.stub.type.FuncConstVarStubElementType
import org.ton.intellij.func.stub.type.FuncFunctionStubElementType
import org.ton.intellij.func.stub.type.FuncGlobalVarStubElementType
import org.ton.intellij.func.stub.type.FuncIncludeDefinitionStubElementType
import org.ton.intellij.util.getChildrenByType
import org.ton.intellij.util.recursionGuard

class FuncFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, FuncLanguage), FuncElement {
    override fun getFileType(): FileType = FuncFileType

    override fun getStub(): FuncFileStub? = super.getStub() as? FuncFileStub

    fun collectIncludedFiles(includeSelf: Boolean = true): Set<FuncFile> {
        return collectIncludedFiles(LinkedHashSet(), includeSelf)
    }

    private fun collectIncludedFiles(collection: MutableSet<FuncFile>, includeSelf: Boolean): MutableSet<FuncFile> {
        recursionGuard(this, false) {
            for (includeDefinition in includeDefinitions) {
                val nextFile = includeDefinition.reference?.resolve()
                if (nextFile !is FuncFile) continue
                nextFile.collectIncludedFiles(collection, true)
            }
            if (includeSelf) {
                collection.add(this)
            }
        }
        return collection
    }

    val includeDefinitions: List<FuncIncludeDefinition>
        get() = CachedValuesManager.getCachedValue(this) {
            val stub = stub
            val children = if (stub != null) getChildrenByType(
                stub,
                FuncElementTypes.INCLUDE_DEFINITION,
                FuncIncludeDefinitionStubElementType.ARRAY_FACTORY
            ) else {
                findChildrenByClass(FuncIncludeDefinition::class.java).toList()
            }
            CachedValueProvider.Result.create(children, this)
        }

    val functions: List<FuncFunction>
        get() = CachedValuesManager.getCachedValue(this) {
            val stub = stub
            val functions = if (stub != null) {
                getChildrenByType(stub, FuncElementTypes.FUNCTION, FuncFunctionStubElementType.ARRAY_FACTORY)
            } else {
                findChildrenByClass(FuncFunction::class.java).toList()
            }
            CachedValueProvider.Result.create(functions, this)
        }

    val constVars: List<FuncConstVar>
        get() = CachedValuesManager.getCachedValue(this) {
            val stub = stub
            val constVars = if (stub != null) {
                getChildrenByType(stub, FuncElementTypes.CONST_VAR, FuncConstVarStubElementType.ARRAY_FACTORY)
            } else {
                findChildrenByClass(FuncConstVarList::class.java).flatMap {
                    it.constVarList
                }
            }
            CachedValueProvider.Result.create(constVars, this)
        }

    val globalVars: List<FuncGlobalVar>
        get() = CachedValuesManager.getCachedValue(this) {
            val stub = stub
            val constVars = if (stub != null) {
                getChildrenByType(stub, FuncElementTypes.GLOBAL_VAR, FuncGlobalVarStubElementType.ARRAY_FACTORY)
            } else {
                findChildrenByClass(FuncGlobalVarList::class.java).flatMap {
                    it.globalVarList
                }
            }
            CachedValueProvider.Result.create(constVars, this)
        }

    fun import(file: FuncFile) {
        if (file == this) return
        val path = VfsUtil.findRelativePath(virtualFile ?: return, file.virtualFile ?: return, '/') ?: return
        val needImport = includeDefinitions.none { it.reference?.resolve() == file }
        if (!needImport) return

        val factory = FuncPsiFactory[project]
        val newInclude = factory.createIncludeDefinition(path)

        tryIncludeAtCorrectLocation(newInclude)
    }

    private fun tryIncludeAtCorrectLocation(includeDefinition: FuncIncludeDefinition) {
        val newline = FuncPsiFactory[project].createNewline()
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

    override fun toString(): String = "FuncFile($name)"
}

private val INCLUDE_COMPARE: Comparator<FuncIncludeDefinition> =
    compareBy(
        {
            it.stringLiteral?.rawString?.text?.count { c -> c == '/' }
        },
        {
            it.stringLiteral?.rawString?.text?.lowercase()
        }
    )
