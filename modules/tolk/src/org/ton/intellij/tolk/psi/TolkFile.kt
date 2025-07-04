package org.ton.intellij.tolk.psi

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.ResolveState
import com.intellij.psi.scope.PsiScopeProcessor
import com.intellij.psi.util.CachedValueProvider.Result
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import org.ton.intellij.tolk.TolkFileType
import org.ton.intellij.tolk.TolkLanguage
import org.ton.intellij.tolk.ide.configurable.tolkSettings
import org.ton.intellij.tolk.psi.impl.resolve
import org.ton.intellij.tolk.psi.impl.resolveFile
import org.ton.intellij.tolk.stub.*
import org.ton.intellij.tolk.stub.type.TolkFunctionStubElementType
import org.ton.intellij.tolk.stub.type.TolkIncludeDefinitionStubElementType

class TolkFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, TolkLanguage), TolkElement {
    override fun getFileType(): FileType = TolkFileType

    override fun getStub(): TolkFileStub? = super.getStub() as? TolkFileStub

    private val cachedDeclarations = CachedValuesManager.getManager(manager.project).createCachedValue({
        val declarations = findDeclarations()
        if (!isPhysical) {
            Result.create(declarations, containingFile, PsiModificationTracker.MODIFICATION_COUNT)
        } else {
            Result.create(declarations, PsiModificationTracker.MODIFICATION_COUNT)
        }
    }, false)

    val includeDefinitions
        get() = withGreenStubOrAst(
            { stub ->
                stub.getChildrenByType(
                    TolkElementTypes.INCLUDE_DEFINITION,
                    TolkIncludeDefinitionStubElementType.ARRAY_FACTORY
                )
            },
            { ast ->
                ast.getChildrenAsPsiElements(
                    TolkElementTypes.INCLUDE_DEFINITION,
                    TolkIncludeDefinitionStubElementType.ARRAY_FACTORY
                )
            }
        )

    val functions
        get() = withGreenStubOrAst(
            { stub ->
                stub.getChildrenByType(TolkElementTypes.FUNCTION, TolkFunctionStubElementType.ARRAY_FACTORY)
            },
            { ast ->
                ast.getChildrenAsPsiElements(TolkElementTypes.FUNCTION, TolkFunctionStubElementType.ARRAY_FACTORY)
            }
        )

    val constVars
        get() = withGreenStubOrAst(
            { stub ->
                stub.getChildrenByType(TolkElementTypes.CONST_VAR, TolkConstVarStub.ARRAY_FACTORY)
            },
            { ast ->
                ast.getChildrenAsPsiElements(TolkElementTypes.CONST_VAR, TolkConstVarStub.ARRAY_FACTORY)
            }
        )

    val globalVars
        get() = withGreenStubOrAst(
            { stub ->
                stub.getChildrenByType(TolkElementTypes.GLOBAL_VAR, TolkGlobalVarStub.ARRAY_FACTORY)
            },
            { ast ->
                ast.getChildrenAsPsiElements(TolkElementTypes.GLOBAL_VAR, TolkGlobalVarStub.ARRAY_FACTORY)
            }
        )

    val typeDefs
        get() = withGreenStubOrAst(
            { stub ->
                stub.getChildrenByType(TolkElementTypes.TYPE_DEF, TolkTypeDefStub.ARRAY_FACTORY)
            },
            { ast ->
                ast.getChildrenAsPsiElements(TolkElementTypes.TYPE_DEF, TolkTypeDefStub.ARRAY_FACTORY)
            }
        )

    val structs
        get() = withGreenStubOrAst(
            { stub ->
                stub.getChildrenByType(TolkElementTypes.STRUCT, TolkStructStub.ARRAY_FACTORY)
            },
            { ast ->
                ast.getChildrenAsPsiElements(TolkElementTypes.STRUCT, TolkStructStub.ARRAY_FACTORY)
            }
        )

    fun resolveSymbols(name: String): Sequence<TolkSymbolElement> {
        val values = cachedDeclarations.value
        return values[name]?.asSequence() ?: emptySequence()
    }

    fun import(file: TolkFile) {
        if (file == this) return
        if (!file.isPhysical) return
        var path = VfsUtil.findRelativePath(virtualFile ?: return, file.virtualFile ?: return, '/') ?: return
        val needImport = includeDefinitions.none { it.resolveFile(it.project) == file.virtualFile }
        if (!needImport) return

        val factory = TolkPsiFactory[project]
        val sdk = project.tolkSettings.toolchain.stdlibDir
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

    override fun processDeclarations(
        processor: PsiScopeProcessor,
        state: ResolveState,
        lastParent: PsiElement?,
        place: PsiElement
    ): Boolean {
        structs.forEach { if (!processor.execute(it, state)) return false }
        typeDefs.forEach { if (!processor.execute(it, state)) return false }
        constVars.forEach { if (!processor.execute(it, state)) return false }
        globalVars.forEach { if (!processor.execute(it, state)) return false }
        functions.forEach { if (!processor.execute(it, state)) return false }
        return true
    }

    private fun findDeclarations(): Map<String, Collection<TolkSymbolElement>> {
        val result = HashMap<String, MutableList<TolkSymbolElement>>()

        val visitedFiles = mutableSetOf<TolkFile>()
        project.tolkSettings.getDefaultImport()?.let {
            if (it != this) {
                visitedFiles.add(it)
                it.findDeclarations().forEach { (name, declarations) ->
                    result.getOrPut(name) { mutableListOf() }.addAll(declarations)
                }
            }
        }
        val processor = object : PsiScopeProcessor {
            override fun execute(
                element: PsiElement,
                state: ResolveState
            ): Boolean {
                if (element is TolkSymbolElement) {
                    val name = element.name ?: return true
                    val declarations = result.getOrPut(name) { mutableListOf() }
                    declarations.add(element)
                }
                return true
            }
        }

        processDeclarations(
            processor,
            state = ResolveState.initial(),
            lastParent = null,
            place = this
        )
        visitedFiles.add(this)
        includeDefinitions.forEach { include ->
            val file = include.resolve() as? TolkFile ?: return@forEach
            if (!visitedFiles.add(file)) return@forEach
            file.processDeclarations(
                processor,
                state = ResolveState.initial(),
                lastParent = null,
                place = this
            )
        }
        return result
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

val VirtualFile.isTolkFile: Boolean get() = fileType == TolkFileType
