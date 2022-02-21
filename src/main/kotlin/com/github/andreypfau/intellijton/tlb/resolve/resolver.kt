package com.github.andreypfau.intellijton.tlb.resolve

import com.github.andreypfau.intellijton.tlb.psi.TlbCombinatorDeclaration
import com.github.andreypfau.intellijton.tlb.psi.TlbElement
import com.github.andreypfau.intellijton.tlb.psi.TlbFile
import com.intellij.psi.util.childrenOfType
import com.intellij.psi.util.findParentOfType

fun TlbElement.resolveFile() = if (this is TlbFile) this else findParentOfType()!!

fun TlbFile.resolveCombinatorDeclarations() = childrenOfType<TlbCombinatorDeclaration>().asSequence()

fun TlbCombinatorDeclaration.resolveImplicitDefinitions() = fieldDefinitionList?.fieldDefinitionList?.asSequence()?.map {
    it.implicitDefinition
}?.filterNotNull() ?: emptySequence()

fun TlbCombinatorDeclaration.resolveImplicitFields() = resolveImplicitDefinitions().map {
    it.implicitField
}.filterNotNull()

fun TlbCombinatorDeclaration.resolveFields() = fieldDefinitionList?.fieldDefinitionList?.asSequence()?.map {
    it.namedField
}?.filterNotNull() ?: emptySequence()