package org.ton.intellij.fc2tolk.tree

import org.ton.intellij.fc2tolk.tree.visitors.FTVisitor
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

interface FTElement {
    val parent: FTElement?

    fun detach(from: FTElement)
    fun attach(to: FTElement)
}

abstract class FTTreeElement : FTElement {
    override var parent: FTElement? = null
    internal val children: ArrayList<Any> = ArrayList()
    private var childNum = 0

    override fun detach(from: FTElement) {
        val prevParent = parent
        require(from == parent) {
            "Incorrect detach: From: $from, Actual: $prevParent"
        }
        parent = null
    }

    override fun attach(to: FTElement) {
        check(parent == null)
        parent = to
    }

    open fun accept(visitor: FTVisitor) = visitor.visitTreeElement(this)

    protected fun <T : FTTreeElement, U : T> child(v: U): ReadWriteProperty<FTTreeElement, T> {
        children.add(childNum, v)
        v.attach(this)
        return FTChild(childNum++)
    }

    protected fun <T : FTTreeElement> children(v: List<T>): ReadWriteProperty<FTTreeElement, List<T>> {
        children.add(childNum, v)
        v.forEach { it.attach(this) }
        return FTListChild(childNum++)
    }

    open fun acceptChildren(visitor: FTVisitor) {
        forEachChild { it.accept(visitor) }
    }

    private inline fun forEachChild(block: (FTTreeElement) -> Unit) {
        children.forEach { child ->
            if (child is FTTreeElement) {
                block(child)
            } else {
                @Suppress("UNCHECKED_CAST")
                (child as? List<FTTreeElement>)?.forEach {
                    block(it)
                }
            }
        }
    }

    private class FTListChild<T : FTElement>(val index: Int) : ReadWriteProperty<FTTreeElement, List<T>> {
        override fun getValue(thisRef: FTTreeElement, property: KProperty<*>): List<T> {
            @Suppress("UNCHECKED_CAST")
            return thisRef.children[index] as List<T>
        }

        override fun setValue(thisRef: FTTreeElement, property: KProperty<*>, value: List<T>) {
            @Suppress("UNCHECKED_CAST")
            (thisRef.children[index] as List<T>).forEach { it.detach(thisRef) }
            thisRef.children[index] = value
            value.forEach { it.attach(thisRef) }
        }
    }

    private class FTChild<T : FTElement>(val index: Int) : ReadWriteProperty<FTTreeElement, T> {
        override fun getValue(thisRef: FTTreeElement, property: KProperty<*>): T {
            @Suppress("UNCHECKED_CAST")
            return thisRef.children[index] as T
        }

        override fun setValue(thisRef: FTTreeElement, property: KProperty<*>, value: T) {
            (thisRef.children[index] as T).detach(thisRef)
            thisRef.children[index] = value
            value.attach(thisRef)
        }
    }
}
