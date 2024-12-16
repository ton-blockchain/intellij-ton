package org.ton.intellij.util

fun <T> Sequence<T>.infiniteWith(with: T): Sequence<T> =
    this + generateSequence { with }

fun <T : Any> Iterator<T>.nextOrNull(): T? =
    if (hasNext()) next() else null

typealias WithNextValue<T> = Pair<T, T?>

fun <T : Any> Sequence<T>.withNext(): Sequence<WithNextValue<T>> = WithNextSequence(this)

private class WithNextSequence<T : Any>(private val sequence: Sequence<T>) : Sequence<WithNextValue<T>> {

    override fun iterator(): Iterator<WithNextValue<T>> = WithNextIterator(sequence.iterator())
}

private class WithNextIterator<T : Any>(private val iterator: Iterator<T>) : Iterator<WithNextValue<T>> {

    private var next: T? = null

    override fun hasNext() = next != null || iterator.hasNext()

    override fun next(): WithNextValue<T> {
        if (next == null) { // The first invocation (or illegal after-the-last invocation)
            next = iterator.next()
        }
        val next = next ?: throw NoSuchElementException()
        val nextNext = iterator.nextOrNull()
        this.next = nextNext
        return WithNextValue(next, nextNext)
    }
}
