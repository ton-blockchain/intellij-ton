/// Simple type alias
type MyInt = int;

/// Generic type alias
type Pair<T, U> = (T, U);

/// Builtin type
type int = builtin;

/// Complex type alias
type ComplexType = fun (int, slice) -> bool;
