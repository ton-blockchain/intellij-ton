/// Generic function with multiple type parameters
fun multiGeneric<T, U, V = int>(first: T, second: U): V {
    return defaultValue;
}

/// Struct with constrained type parameter
struct GenericStruct<T = slice> {
    value: T
}
