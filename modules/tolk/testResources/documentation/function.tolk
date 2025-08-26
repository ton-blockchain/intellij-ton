/// Simple function without parameters
fun simpleFunction(): void {
}

/// Function with parameters
/// @param x The first parameter
/// @param y The second parameter
fun addNumbers(x: int, y: int): int {
    return x + y;
}

/// Function with receiver
fun Point.distance(self, other: Point): int {
    return 0;
}

/// Function with mutate parameter
fun modifyValue(mutate value: int): void {
    value = 10;
}

/// Generic function
fun identity<T>(value: T): T {
    return value;
}

/// Function with annotation
@method_id(123)
fun annotatedFunction(): void {
}
