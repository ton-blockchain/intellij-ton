package org.ton.intellij.func.type

import com.intellij.openapi.util.Key
import org.ton.intellij.func.type.FuncType.Type.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max
import kotlin.math.min

class FuncType {
    var type: Type
        private set
    var value: Int = 0
        private set
    var width: Width = Width()
        private set
    var args: Array<FuncType> = emptyArray()
        private set

    private constructor(
        type: Type,
        value: Int,
    ) {
        this.type = type
        this.value = value
    }

    private constructor(
        type: Type,
        value: Int,
        width: Int,
    ) {
        this.type = type
        this.value = value
        this.width = Width(width)
    }

    private constructor(
        type: Type,
        args: List<FuncType>,
    ) {
        this.type = type
        this.args = args.toTypedArray()
        this.value = args.size
        computeWidth()
    }

    fun isAtomic() = type == ATOMIC

    fun isAtomic(type: Atomic) = isAtomic() && value == type.ordinal

    fun isInt() = isAtomic(Atomic.INT)

    fun isVar() = type == VAR

    override fun toString(): String = buildString {
        toString(this)
    }

    private fun toString(sb: StringBuilder, lexLevel: Int = 0) {
        sb.apply {
            when (type) {
                UNKNOWN -> append("??").append(value)
                VAR -> when (value) {
                    in -26 until 0 -> append('_').append((91 + value).toChar())
                    in 0..25 -> append((65 + value).toChar())
                    else -> append("TVAR").append(value)
                }

                INDIRECT -> args[0].toString(this)
                ATOMIC -> append(Atomic[value]?.name?.lowercase() ?: "atomic-type-$value")
                TENSOR -> {
                    if (lexLevel > -127) append('(')
                    var size = args.size
                    if (size != 0) {
                        args.forEach {
                            it.toString(this)
                            if (--size != 0) append(", ")
                        }
                    }
                    if (lexLevel > -127) append(')')
                }

                TUPLE -> {
                    append('[')
                    var size = args.size
                    if (size == 1 && args[0].type == TENSOR) {
                        args[0].toString(this, -127)
                    } else if (size != 0) {
                        args.forEach {
                            it.toString(this)
                            if (--size != 0) append(", ")
                        }
                    }
                    append(']')
                }

                MAP -> {
                    check(args.size == 2)
                    if (lexLevel > 0) append('(')
                    args[0].toString(this, 1)
                    append(" -> ")
                    args[1].toString(this)
                    if (lexLevel > 0) append(')')
                }

                FORALL -> {
                    check(args.isNotEmpty())
                    if (lexLevel > 0) append('(')
                    append("Forall")
                    for (i in 1 until args.size) {
                        append(if (i > 1) ' ' else '(')
                        args[i].toString(this)
                    }
                    append(") ")
                    args[0].toString(this)
                    if (lexLevel > 0) append(')')
                }
            }
        }
    }

    private fun computeWidth() {
        width = when (type) {
            TENSOR -> {
                var min = 0
                var max = 0
                args.forEach {
                    min += it.width.min
                    max += it.width.max
                }
                if (min > Width.INF) {
                    min = Width.INF
                }
                if (max > Width.INF) {
                    max = Width.INF
                }
                Width(min, max)
            }

            TUPLE -> {
                args.forEach {
                    it.computeWidth()
                }
                Width(1)
            }

            ATOMIC, MAP -> Width(1)
            INDIRECT -> args[0].width
            else -> Width()
        }
    }

    private fun recomputeWidth(): Boolean {
        when (type) {
            TENSOR, INDIRECT -> {
                var min = 0
                var max = 0
                args.forEach {
                    min += it.width.min
                    max += it.width.max
                }
                if (min > width.max || max < width.min) {
                    return false
                }
                if (min > Width.INF) {
                    min = Width.INF
                }
                if (max > Width.INF) {
                    max = Width.INF
                }
                if (width.min < min) {
                    width = width.copy(min = min)
                }
                if (width.max > max) {
                    width = width.copy(max = max)
                }
                return true
            }

            TUPLE -> {
                args.forEach {
                    if (it.width.min > 1 || it.width.max < 1 || it.width.min > it.width.max) {
                        return false
                    }
                }
                return true
            }

            else -> return false
        }
    }

    private fun replaceWidth(te2: FuncType) {
        if (te2 == this) return
        type = INDIRECT
        value = 0
        width = te2.width
        args = arrayOf(te2)
    }

    enum class Type {
        UNKNOWN, VAR, INDIRECT, ATOMIC, TENSOR, TUPLE, MAP, FORALL
    }

    enum class Atomic {
        INT, CELL, SLICE, BUILDER, CONT, TUPLE, TYPE;

        override fun toString(): String = name.lowercase()

        fun funcType(): FuncType = atomic(this)

        companion object {
            private val values = values()

            operator fun get(ordinal: Int): Atomic? = values.getOrNull(ordinal)
        }
    }

    data class Width(
        val min: Int,
        val max: Int,
    ) {
        constructor() : this(0, INF)
        constructor(width: Int) : this(width, width)

        override fun toString(): String = buildString {
            append(min)
            if (min != max) {
                append("..")
                if (max < INF) {
                    append(max)
                }
            }
        }

        companion object {
            const val INF = 1023
        }
    }

    companion object {
        val KEY = Key.create<FuncType>("FUNC_TYPE")

        private val holes = AtomicInteger()
        private val typeVars = AtomicInteger()

        fun hole() = FuncType(UNKNOWN, holes.incrementAndGet())

        fun unit() = FuncType(TENSOR, 0, 0)

        fun atomic(value: Atomic) = FuncType(ATOMIC, value.ordinal, 1)

        fun tensor(list: List<FuncType>, red: Boolean = true) =
            if (red && list.size == 1) list.first() else FuncType(TENSOR, list)

        fun typeVar() = FuncType(VAR, typeVars.decrementAndGet(), 1)

        fun map(from: FuncType, to: FuncType) = FuncType(MAP, listOf(from, to))

        fun forall(list: List<FuncType>, body: FuncType) = FuncType(FORALL, listOf(body) + list)

        fun removeIndirect(
            teGetter: () -> FuncType,
            teSetter: (FuncType) -> Unit,
            forbidden: FuncType? = null,
        ): Boolean {
            while (teGetter().type == INDIRECT) {
                teSetter(teGetter().args[0])
            }
            val type = teGetter()
            if (type.type == UNKNOWN) {
                return type != forbidden
            }
            var res = true

            for (i in type.args.indices) {
                res = res && removeIndirect(
                    teGetter = { type.args[i] },
                    teSetter = { type.args[i] = it },
                    forbidden
                )
            }
            return res
        }

        private fun checkWidthCompat(te1: FuncType, te2: FuncType) {
            if (te1.width.min > te2.width.max || te2.width.min > te1.width.max) {
                throw FuncUnifyTypeException(te1, te2, "Can't unify types of width ${te1.width} and ${te2.width}")
            }
        }

        private fun checkUpdateWidths(te1: FuncType, te2: FuncType) {
            checkWidthCompat(te1, te2)
            val width = Width(
                min = max(te1.width.min, te2.width.min),
                max = min(te1.width.max, te2.width.max)
            )
            te1.width = width
            te2.width = width
            check(width.min <= width.max)
        }

        fun unify(
            getTe1: () -> FuncType,
            setTe1: (FuncType) -> Unit,
            getTe2: () -> FuncType,
            setTe2: (FuncType) -> Unit,
        ) {
            var te1 = getTe1()
            var te2 = getTe2()
            println("Start unifying: $te1 and $te2")
            try {
                while (te1.type == INDIRECT) {
                    te1 = te1.args[0]
                }
                while (te2.type == INDIRECT) {
                    te2 = te2.args[0]
                }
                if (te1 == te2) return
                if (te1.type == UNKNOWN) {
                    if (te2.type == UNKNOWN) {
                        check(te1.value != te2.value)
                    }
                    if (!removeIndirect(
                            teGetter = { te2 },
                            teSetter = { te2 = it },
                            forbidden = te1
                        )
                    ) {
                        throw FuncUnifyTypeException(te1, te2, "type unification results in an infinite cyclic type")
                    }
                    checkUpdateWidths(te1, te2)
                    te1.replaceWidth(te2)
                    te1 = te2
                    return
                }
                if (te2.type == UNKNOWN) {
                    if (!removeIndirect(
                            teGetter = { te1 },
                            teSetter = { te1 = it },
                            forbidden = te2
                        )
                    ) {
                        throw FuncUnifyTypeException(te2, te1, "type unification results in an infinite cyclic type")
                    }
                    checkUpdateWidths(te2, te1)
                    te2.replaceWidth(te1)
                    te2 = te1
                    return
                }
                if (te1.type != te2.type || te1.value != te2.value || te1.args.size != te2.args.size) {
                    throw FuncUnifyTypeException(te1, te2)
                }

                repeat(te1.args.size) { i ->
                    unify(
                        getTe1 = { te1.args[i] },
                        setTe1 = { te1.args[i] = it },
                        getTe2 = { te2.args[i] },
                        setTe2 = { te2.args[i] = it }
                    )
                }

                if (te1.type == TENSOR) {
                    if (!te1.recomputeWidth()) {
                        throw FuncUnifyTypeException(
                            te1,
                            te2,
                            "type unification incompatible with known width of first type"
                        )
                    }
                    if (!te2.recomputeWidth()) {
                        throw FuncUnifyTypeException(
                            te2,
                            te1,
                            "type unification incompatible with known width of first type"
                        )
                    }
                    checkUpdateWidths(te1, te2)
                }

                te1.replaceWidth(te2)
                te1 = te2
            } finally {
                setTe1(te1)
                setTe2(te2)
            }
        }
    }
}

class FuncUnifyTypeException(
    val te1: FuncType,
    val te2: FuncType,
    message: String = "",
) : RuntimeException(buildString {
    append("Can't unify type ")
    append(te1)
    append(" with ")
    append(te2)
    if (message.isNotEmpty()) {
        append(": ")
        append(message)
    }
})
