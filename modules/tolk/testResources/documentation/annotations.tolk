/// Function with inline annotation
@inline
fun inlineFunction(): int {
    return 42;
}

/// Function with inline_ref annotation
@inline_ref
fun inlineRefFunction(): int {
    return 24;
}

/// Function with noinline annotation
@noinline
fun noinlineFunction(): int {
    return 100;
}

/// Function with pure annotation
@pure
fun pureFunction(x: int): int {
    return x * 2;
}

/// Deprecated function
@deprecated("Use newFunction() instead")
fun oldFunction(): void {
}

/// Function with overflow policy
@overflow1023_policy("suppress")
fun functionWithOverflowPolicy(): void {
}

/// Function with bounced message policy
@on_bounced_policy("manual")
fun functionWithBouncedPolicy(): void {
}

/// Function with method ID
@method_id(123)
fun getBalance(): int {
    return 0;
}

/// Function with multiple annotations
@method_id(456)
@inline
@pure
fun multiAnnotatedFunction(): int {
    return 1;
}

/// Constant with deprecated annotation
@deprecated("Use NEW_CONST instead")
const OLD_CONST = 42;
