package org.ton.intellij.util.hm

class UnificationException(val t1: HMType, val t2: HMType) : IllegalArgumentException("Cannot unify $t1 and $t2")

// Type Representation
sealed class HMType {
    data class Var(val name: String) : HMType()         // Type variable
    data class Func(val from: HMType, val to: HMType) : HMType() // Function type
    data class Const<T>(val value: T) : HMType()       // Constant type (e.g., Int, Bool)
    data class ForAll(val vars: List<String>, val body: HMType) : HMType() {
        // Instantiate: Replace quantified variables with fresh type variables
        fun instantiate(typeGen: TypeGenerator): HMType {
            val freshSubs = vars.associateWith { typeGen.fresh() }
            return body.substitute(HMSubstitution(freshSubs))
        }
    } // Polymorphic type
    data class Tuple(val elements: List<HMType>) : HMType() // Tuple type
    data class Tensor(val elements: List<HMType>) : HMType() // Tuple type

    infix fun unify(other: HMType): HMSubstitution = unify(this, other)

    fun substitute(sub: HMSubstitution): HMType = substitute(this, sub)

    companion object {
        // Unification: Solves constraints
        fun unify(t1: HMType, t2: HMType): HMSubstitution {
            return when {
                t1 == t2 -> HMSubstitution()
                t1 is Var -> HMSubstitution.bind(t1.name, t2)
                t2 is Var -> HMSubstitution.bind(t2.name, t1)
                t1 is Func && t2 is Func -> {
                    val s1 = unify(t1.from, t2.from)
                    val s2 = unify(substitute(t1.to, s1), substitute(t2.to, s1))
                    s1 + s2
                }
                t1 is Tensor && t2 is Tensor -> {
                    if (t1.elements.size != t2.elements.size)
                        throw IllegalArgumentException("Cannot unify tuples of different sizes: $t1 and $t2")
                    t1.elements.zip(t2.elements).fold(HMSubstitution()) { acc, (e1, e2) ->
                        acc + unify(substitute(e1, acc), substitute(e2, acc))
                    }
                }
                t1 is Tuple && t2 is Tuple -> {
                    if (t1.elements.size != t2.elements.size)
                        throw IllegalArgumentException("Cannot unify tuples of different sizes: $t1 and $t2")
                    t1.elements.zip(t2.elements).fold(HMSubstitution()) { acc, (e1, e2) ->
                        acc + unify(substitute(e1, acc), substitute(e2, acc))
                    }
                }
                else -> throw UnificationException(t1, t2)
            }
        }

        // Substitute type variables
        fun substitute(type: HMType, sub: HMSubstitution): HMType = when (type) {
            is Var -> sub[type.name] ?: type
            is Func -> Func(
                substitute(type.from, sub),
                substitute(type.to, sub)
            )
            is Tuple -> Tuple(type.elements.map { substitute(it, sub) })
            is Tensor -> Tensor(type.elements.map { substitute(it, sub) })
            is Const<*> -> type
            is ForAll -> ForAll(type.vars, substitute(type.body, sub))
        }
    }
}

// Substitution Map
class HMSubstitution(
    val values: Map<String, HMType> = emptyMap()
) {
    operator fun get(name: String): HMType? = values[name]

    operator fun plus(other: HMSubstitution): HMSubstitution = composeSubs(this, other)

    companion object {
        // Helper: Compose Substitutions
        fun composeSubs(s1: HMSubstitution, s2: HMSubstitution): HMSubstitution {
            val newSub = s2.values.mapValues { (_, v) -> v.substitute(s1) }
            return HMSubstitution(s1.values + newSub)
        }

        // Bind a type variable to a type
        fun bind(name: String, type: HMType): HMSubstitution {
            if (type == HMType.Var(name)) throw IllegalArgumentException("Occurs check failed: $name in $type")
            return HMSubstitution(mapOf(name to type))
        }
    }
}

// Type Environment
typealias HMTypeEnv = MutableMap<String, HMType>

// Generate fresh type variables
class TypeGenerator {
    private var counter = 0
    fun fresh(): HMType.Var = HMType.Var("t${counter++}")
}

//// Generalize: Convert a type into a polymorphic type
//fun generalize(env: HMTypeEnv, type: HMType): HMType {
//    val envVars = env.values.flatMap { freeTypeVars(it) }.toSet()
//    val typeVars = freeTypeVars(type)
//    val generalizedVars = typeVars - envVars
//    return if (generalizedVars.isEmpty()) type
//    else HMType.ForAll(generalizedVars.toList(), type)
//}
//
//// Extract free type variables from a type
//fun freeTypeVars(type: HMType): Set<String> = when (type) {
//    is HMType.Var -> setOf(type.name)
//    is HMType.Func -> freeTypeVars(type.from) + freeTypeVars(type.to)
//    is HMType.Tuple -> type.elements.flatMap { freeTypeVars(it) }.toSet()
//    is HMType.Tensor -> type.elements.flatMap { freeTypeVars(it) }.toSet()
//    is HMType.Const<*> -> emptySet()
//    is HMType.ForAll -> freeTypeVars(type.body) - type.vars.toSet()
//}

// Hindley–Milner Algorithm
class HindleyMilner {
    val typeGen = TypeGenerator()

    // Infer types for expressions
    fun infer(env: HMTypeEnv, expr: HMExpr): Pair<HMSubstitution, HMType> = when (expr) {
        is HMExpr.Var -> {
            val type = env[expr.name]
                ?: throw IllegalArgumentException("Unbound variable: ${expr.name}")
            when (type) {
                is HMType.ForAll -> HMSubstitution() to type.instantiate(typeGen)
                else -> HMSubstitution() to type
            }
        }

        is HMExpr.App -> {
            val (s1, funcType) = infer(env, expr.func)
            val (s2, argType) = infer(substituteEnv(env, s1), expr.arg)
            val resultType = typeGen.fresh()
            val s3 = funcType.substitute(s2) unify HMType.Func(argType, resultType)
            ((s3 + s2) + s1) to resultType.substitute(s3)
        }

        is HMExpr.Const -> HMSubstitution() to expr.type
        is HMExpr.Tuple -> {
            val inferred = expr.elements.map { infer(env, it) }
            val substitution = inferred.fold(HMSubstitution()) { acc, (sub, _) -> acc + sub }
            val types = inferred.map { (_, type) -> type.substitute(substitution) }
            substitution to HMType.Tuple(types)
        }
        is HMExpr.Tensor -> {
            val inferred = expr.elements.map { infer(env, it) }
            val substitution = inferred.fold(HMSubstitution()) { acc, (sub, _) -> acc + sub }
            val types = inferred.map { (_, type) -> type.substitute(substitution) }
            substitution to HMType.Tensor(types)
        }
    }

    // Apply substitution to environment
    private fun substituteEnv(env: HMTypeEnv, sub: HMSubstitution): HMTypeEnv =
        env.mapValues { (_, type) -> type.substitute(sub) }.toMutableMap()
}

// AST Representation for Expressions
sealed class HMExpr {
    data class Var(val name: String) : HMExpr()
    data class App(val func: HMExpr, val arg: HMExpr) : HMExpr()
    data class Const(val type: HMType) : HMExpr()
    data class Tuple(val elements: List<HMExpr>) : HMExpr() // Tuple creation
    data class Tensor(val elements: List<HMExpr>) : HMExpr() // Tuple creation
}

fun main() {
    val env = mutableMapOf<String, HMType>()
    val hm = HindleyMilner()

    /*
    fun test<T>(a: (int, T), b: bool) -> (T, bool)

    val (a: int, b) = test((int, int), bool)
    */

   val func = HMType.ForAll(
        listOf("T"),
        HMType.Func(
            HMType.Tensor(
                listOf(HMType.Const("Int"), HMType.Var("T"))
            ),
            HMType.Tensor(listOf(HMType.Var("T"), HMType.Const("Bool")))
        ),
    )

}

//
//val hm = HindleyMilner()
//val env = mutableMapOf<String, HMType>()
//
//env.put("aToInt", HMType.ForAll(
//    listOf("a"),
//    HMType.Func(
//        HMType.Var("a"),
//        HMType.Tuple(listOf(HMType.Const("Int"), HMType.Var("a")))
//    )
//))
//env.put("AddToList", HMType.ForAll(
//    listOf("a"),
//    HMType.Func(
//
//    )
//))
//
//val expr = HindleyMilnerExpr.App(
//    HindleyMilnerExpr.Var("aToInt"),
//    HindleyMilnerExpr.Const(HMType.Const("None"))
//)
//
//hm.infer(env, expr)