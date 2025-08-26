/// Function returning nullable type
fun maybeValue(): int? {
    return null;
}

/// Function with complex union
fun multiUnion(): int | slice | bool | null {
    return 42;
}

/// Simple union parameter
fun processUnion(value: slice | int): void {
}
