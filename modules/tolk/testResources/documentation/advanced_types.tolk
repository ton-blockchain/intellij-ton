/// Function with advanced type features
fun int.advancedTypes<T, U = int>(
    /// Self parameter
    self,
    /// Generic parameter
    generic: T,
    /// Nullable generic
    nullable: T?,
    /// Function type parameter  
    callback: fun (T) -> U,
    /// Tensor with mixed types
    mixed: (int, T, slice),
    /// Typed tuple
    typed: [T, U, bool]
): (T, U) {
    return (generic, default);
}

/// Complex union type
type Result<T, E> = T | E;

/// Type with constraints
type Numeric = int | uint64 | coins;
