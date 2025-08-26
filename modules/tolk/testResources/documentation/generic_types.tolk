/// Generic struct with multiple parameters
struct Pair<T, U> {
    first: T,
    second: U
}

/// Function with generic constraints
fun process<T>(value: T, default: T = value): T {
    return default;
}

/// Nested generics
fun nested<T>(): Pair<T, Pair<int, T>> {
    return Pair { first: value, second: Pair { first: 0, second: value } };
}
