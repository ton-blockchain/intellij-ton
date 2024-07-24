package org.ton.intellij.tlb.resolve

import com.intellij.psi.util.childrenOfType
import org.ton.intellij.tlb.TlbFileType
import org.ton.intellij.tlb.psi.TlbElement
import org.ton.intellij.tlb.psi.TlbFile
import org.ton.intellij.util.psiManager

fun TlbElement.resolveFile() = if (this is TlbFile) this else containingFile as TlbFile

//fun TlbFile.resolveCombinatorDeclarations() = childrenOfType<TlbCombinatorDeclaration>().asSequence()
//fun TlbFile.resolveAllCombinatorDeclarations() =
//    virtualFile.parent?.children?.asSequence()?.filter { file ->
//        file.extension.equals(TlbFileType.defaultExtension, ignoreCase = true)
//    }?.map { file ->
//        project.psiManager.findFile(file) as? TlbFile
//    }?.filterNotNull()?.flatMap { file ->
//        file.resolveCombinatorDeclarations()
//    } ?: emptySequence()
//
//fun TlbCombinatorDeclaration.resolveImplicitDefinitions() =
//    fieldDefinitionList?.fieldDefinitionList?.asSequence()?.map {
//        it.implicitDefinition
//    }?.filterNotNull() ?: emptySequence()
//
//fun TlbAnonymousConstructor?.resolveImplicitDefinitions() =
//    this?.fieldDefinitionList?.fieldDefinitionList?.asSequence()?.map {
//        it.implicitDefinition
//    }?.filterNotNull() ?: emptySequence()
//
//fun TlbCombinatorDeclaration.resolveImplicitFields() = resolveImplicitDefinitions().map {
//    it.implicitField
//}.filterNotNull()
//
//fun TlbAnonymousConstructor?.resolveImplicitFields() = resolveImplicitDefinitions().map {
//    it.implicitField
//}.filterNotNull()
//
//fun TlbCombinatorDeclaration.resolveFields() =
//    fieldDefinitionList?.fieldDefinitionList?.asSequence()?.map {
//        it.namedField
//    }?.filterNotNull() ?: emptySequence()
//
//fun TlbAnonymousConstructor?.resolveFields() =
//    this?.fieldDefinitionList?.fieldDefinitionList?.asSequence()?.map {
//        it.namedField
//    }?.filterNotNull() ?: emptySequence()
