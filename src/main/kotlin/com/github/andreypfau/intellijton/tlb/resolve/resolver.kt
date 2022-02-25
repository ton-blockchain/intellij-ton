package com.github.andreypfau.intellijton.tlb.resolve

import com.github.andreypfau.intellijton.psiManager
import com.github.andreypfau.intellijton.tlb.TlbFileType
import com.github.andreypfau.intellijton.tlb.psi.TlbCombinatorDeclaration
import com.github.andreypfau.intellijton.tlb.psi.TlbElement
import com.github.andreypfau.intellijton.tlb.psi.TlbFile
import com.intellij.psi.util.childrenOfType

fun TlbElement.resolveFile() = if (this is TlbFile) this else containingFile as TlbFile

fun TlbFile.resolveCombinatorDeclarations() = childrenOfType<TlbCombinatorDeclaration>().asSequence()
fun TlbFile.resolveAllCombinatorDeclarations() =
    virtualFile.parent.children.asSequence().filter { file ->
        file.extension.equals(TlbFileType.defaultExtension, ignoreCase = true)
    }.map { file ->
        project.psiManager.findFile(file) as? TlbFile
    }.filterNotNull().flatMap { file ->
        file.resolveCombinatorDeclarations()
    }

fun TlbCombinatorDeclaration.resolveImplicitDefinitions() =
    fieldDefinitionList?.fieldDefinitionList?.asSequence()?.map {
        it.implicitDefinition
    }?.filterNotNull() ?: emptySequence()

fun TlbCombinatorDeclaration.resolveImplicitFields() = resolveImplicitDefinitions().map {
    it.implicitField
}.filterNotNull()

fun TlbCombinatorDeclaration.resolveFields() = fieldDefinitionList?.fieldDefinitionList?.asSequence()?.map {
    it.namedField
}?.filterNotNull() ?: emptySequence()