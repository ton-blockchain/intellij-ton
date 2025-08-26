/// Function with complex types
fun complexFunction(
    tensor: (int, slice, bool),
    tuple: [int, slice],
    nullable: int?,
    function_param: fun(int) -> slice
): (int, slice) {
    return (0, "");
}

/// Variable with union type
fun unionExample(): int | slice | null {
    return 42;
}
