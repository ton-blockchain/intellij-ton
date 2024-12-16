package org.ton.intellij.fc2tolk.tree.visitors

import org.ton.intellij.fc2tolk.tree.*

abstract class FTVisitor {
    abstract fun visitTreeElement(element: FTElement)
    open fun visitTreeRoot(treeRoot: FTTreeRoot) = visitTreeElement(treeRoot)
    open fun visitNameIdentifier(nameIdentifier: FTNameIdentifier) = visitTreeElement(nameIdentifier)
    open fun visitFile(file: FTFile) = visitTreeElement(file)
    open fun visitTypeParameter(typeParameter: FTTypeParameter) = visitTreeElement(typeParameter)
    open fun visitTypeParameterList(typeParameterList: FTTypeParameterList) = visitTreeElement(typeParameterList)
    open fun visitFunction(function: FTFunction) = visitTreeElement(function)
    open fun visitTypeElement(ftTypeElement: FTTypeElement) = visitTreeElement(ftTypeElement)
    open fun visitMethodId(methodId: FTMethodId) = visitTreeElement(methodId)
}