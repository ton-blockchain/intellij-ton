/// Simple struct
struct Point {
    /// X coordinate
    x: int,
    /// Y coordinate with default value
    y: int = 0
}

/// Generic struct
struct Container<T> {
    /// The contained value
    value: T
}

/// Struct with constructor tag
struct(123) TaggedStruct {
    /// Field with complex type
    data: [int, slice]
}

/// Empty struct
struct Empty {
}
