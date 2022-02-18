package com.github.andreypfau.intellijton.tlb.psi

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement

interface TlbElement : PsiElement
abstract class TlbElementImpl(node: ASTNode): ASTWrapperPsiElement(node)