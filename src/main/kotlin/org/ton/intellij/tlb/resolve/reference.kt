package org.ton.intellij.tlb.resolve

import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.util.elementType
import com.intellij.psi.util.parentOfType
import org.ton.intellij.tlb.psi.*
import org.ton.tlb.compiler.TlbTypeExpression


//class TlbNamedRefReference(
//    element: TlbNamedRef,
//) : TlbReferenceBase<TlbNamedRef>(element) {
//    override fun multiResolve(): Sequence<TlbElement> {
////        if (element.parent !is TlbTypeExpression) return emptySequence()
//
////        val anonymousConstructor = element.parentOfType<TlbAnonymousConstructor>()
////        val currentCombinatorDeclaration =
////            element.parentOfType<TlbCombinatorDeclaration>() ?: return resolveCombinators()
////
////        val fields =
////            resolveFields(anonymousConstructor) + resolveFields(currentCombinatorDeclaration)
////        if (fields.isNotEmpty()) return fields.asSequence()
////
////        val implicitFields =
////            resolveImplicitFields(anonymousConstructor) + resolveImplicitFields(currentCombinatorDeclaration)
////        if (implicitFields.isNotEmpty()) return implicitFields.asSequence()
////
////        return resolveCombinators()
//        return emptySequence()
//    }
//
////    private fun resolveFields(combinatorDeclaration: TlbCombinatorDeclaration) =
////        combinatorDeclaration.resolveFields().filter { namedField ->
////            namedField.fieldName.textMatches(element)
////        }.toList()
////
////    private fun resolveFields(anonymousConstructor: TlbAnonymousConstructor?) =
////        anonymousConstructor.resolveFields().filter { namedField ->
////            namedField.fieldName.textMatches(element)
////        }.toList()
////
////    private fun resolveImplicitFields(combinatorDeclaration: TlbCombinatorDeclaration) =
////        combinatorDeclaration.resolveImplicitFields().filter { implicitField ->
//////            implicitField.implicitFieldName.textMatches(element)
////            false
////        }.toList()
////
////    private fun resolveImplicitFields(anonymousConstructor: TlbAnonymousConstructor?) =
////        anonymousConstructor.resolveImplicitFields().filter { implicitField ->
//////            implicitField.implicitFieldName.textMatches(element)
////            false
////        }.toList()
////
////    private fun resolveCombinators() = element.resolveFile()
////        .resolveAllCombinatorDeclarations()
////        .map {
////            it.combinator?.combinatorName
////        }
////        .filterNotNull()
////        .filter {
////            it.textMatches(element.identifier)
////        }
//}
