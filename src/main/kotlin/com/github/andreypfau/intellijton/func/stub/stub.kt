package com.github.andreypfau.intellijton.func.stub

import com.github.andreypfau.intellijton.func.FuncLanguage
import com.github.andreypfau.intellijton.func.psi.*
import com.github.andreypfau.intellijton.func.psi.impl.FuncFunctionImpl
import com.github.andreypfau.intellijton.func.resolve.FuncReference
import com.github.andreypfau.intellijton.func.resolve.FuncReferenceBase
import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.ide.projectView.PresentationData
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.StubBuilder
import com.intellij.psi.stubs.*
import com.intellij.psi.tree.IStubFileElementType

fun factory(name: String): FuncStubElementType<*, *> = when (name) {
    "FUNCTION" -> FuncFunctionStub.Type
    else -> error("Can't reFuncve stub for $name")
}

interface FuncNamedStub {
    val name: String?
}

abstract class FuncStubElementType<S : StubElement<*>, P : FuncElement>(
    debugName: String
) : IStubElementType<S, P>(debugName, FuncLanguage) {
    override fun getExternalId(): String = "func.${super.toString()}"
}

abstract class FuncStubbedElementImpl<StubT : StubElement<*>> : StubBasedPsiElementBase<StubT>, FuncElement {
    constructor(node: ASTNode) : super(node)
    constructor(stub: StubT, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

    override fun getReference(): FuncReference? = null
    override fun toString(): String = "${javaClass.simpleName}($elementType)"
}

abstract class FuncStubbedNamedElementImpl<S> :
    FuncStubbedElementImpl<S>,
    FuncNamedElement,
    PsiNameIdentifierOwner where S : FuncNamedStub, S : StubElement<*> {

    constructor(node: ASTNode) : super(node)
    constructor(stub: S, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

    override fun getNameIdentifier(): PsiElement? = findChildByType(FuncTokenTypes.IDENTIFIER)
    override fun getName() = stub?.name ?: nameIdentifier?.text
    override fun setName(name: String): PsiElement? = apply {
        nameIdentifier?.replace(project.funcPsiFactory.createIdentifier(name))
    }

    override fun getNavigationElement(): PsiElement = nameIdentifier ?: this
    override fun getTextOffset(): Int = nameIdentifier?.textOffset ?: super.getTextOffset()
    override fun getPresentation() = PresentationData(name, "", getIcon(0), null)
    override fun getReference(): FuncReferenceBase<*>? = null
}

class FuncFileStub(file: FuncFile?) : PsiFileStubImpl<FuncFile>(file) {
    override fun getType() = Type

    object Type : IStubFileElementType<FuncFileStub>(FuncLanguage) {
        // bump version every time stub tree changes
        override fun getStubVersion() = 1
        override fun getBuilder(): StubBuilder = object : DefaultStubBuilder() {
            override fun createStubForFile(file: PsiFile) = FuncFileStub(file as FuncFile)
        }

        override fun serialize(stub: FuncFileStub, dataStream: StubOutputStream) {}
        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?) = FuncFileStub(null)
        override fun getExternalId(): String = "func.file"
    }
}

class FuncFunctionStub(
    parent: StubElement<*>?,
    elementType: IStubElementType<*, *>,
    override val name: String?
) : StubBase<FuncFunction>(parent, elementType), FuncNamedStub {
    object Type : FuncStubElementType<FuncFunctionStub, FuncFunction>("FUNCTION") {
        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?) =
            FuncFunctionStub(parentStub, this, dataStream.readNameString())

        override fun serialize(stub: FuncFunctionStub, dataStream: StubOutputStream) = with(dataStream) {
            writeName(stub.name)
        }

        override fun createPsi(stub: FuncFunctionStub) = FuncFunctionImpl(stub, this)

        override fun createStub(psi: FuncFunction, parentStub: StubElement<*>?) =
            FuncFunctionStub(parentStub, this, psi.name)

        override fun indexStub(stub: FuncFunctionStub, sink: IndexSink) = sink.indexFunctionDef(stub)
    }
}
