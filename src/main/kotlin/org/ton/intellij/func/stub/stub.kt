package org.ton.intellij.func.stub

import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.ide.projectView.PresentationData
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.StubBuilder
import com.intellij.psi.stubs.*
import com.intellij.psi.tree.IStubFileElementType
import org.ton.intellij.func.FuncLanguage
import org.ton.intellij.func.psi.*
import org.ton.intellij.func.psi.impl.FuncFunctionImpl
import org.ton.intellij.func.psi.impl.FuncIncludePathImpl
import org.ton.intellij.func.resolve.FuncReference
import org.ton.intellij.func.resolve.FuncReferenceBase

fun factory(name: String): FuncStubElementType<*, *> = when (name) {
    "FUNCTION" -> FuncFunctionStub.Type
    "INCLUDE_PATH" -> FuncIncludePathStub.Type
    else -> error("Can't find stub for $name")
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
        override fun getStubVersion() = 2
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

class FuncIncludePathStub(
    parent: StubElement<*>?,
    elementType: IStubElementType<*, *>,
    override val name: String?,
    val path: String?
) : StubBase<FuncIncludePathImpl>(parent, elementType), FuncNamedStub {
    object Type : FuncStubElementType<FuncIncludePathStub, FuncIncludePathImpl>("INCLUDE_PATH") {
        override fun serialize(stub: FuncIncludePathStub, dataStream: StubOutputStream) = with(dataStream) {
            writeName(stub.name)
            writeUTFFast(stub.path ?: "")
        }

        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): FuncIncludePathStub =
            FuncIncludePathStub(parentStub, this, dataStream.readName().toString(), dataStream.readUTFFast())

        override fun createStub(
            psi: FuncIncludePathImpl,
            parentStub: StubElement<out PsiElement>?
        ): FuncIncludePathStub = FuncIncludePathStub(parentStub, this, psi.name, psi.text)

        override fun createPsi(stub: FuncIncludePathStub): FuncIncludePathImpl = FuncIncludePathImpl(stub, this)

        override fun indexStub(stub: FuncIncludePathStub, sink: IndexSink) = sink.indexIncludePathDef(stub)
    }
}