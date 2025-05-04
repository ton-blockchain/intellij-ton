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

fun <K, V> mergeMaps(map1: Map<K, V>, map2: Map<K, V>): Map<K, V> =
    when {
        map1.isEmpty() -> map2
        map2.isEmpty() -> map1
        else -> newHashMapWithExpectedSize<K, V>(map1.size + map2.size).apply {
            putAll(map1)
            putAll(map2)
        }
    }

private fun <K, V> newHashMapWithExpectedSize(size: Int): java.util.HashMap<K, V> =
    HashMap<K, V>(mapCapacity(size))

private const val INT_MAX_POWER_OF_TWO: Int = Int.MAX_VALUE / 2 + 1

private fun mapCapacity(expectedSize: Int): Int {
    if (expectedSize < 3) {
        return expectedSize + 1
    }
    if (expectedSize < INT_MAX_POWER_OF_TWO) {
        return expectedSize + expectedSize / 3
    }
    return Int.MAX_VALUE
}
