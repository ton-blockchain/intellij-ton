/// Function with multiple annotations
@method_id(123)
@inline
@pure
fun annotatedFunction(): void {
}

/// Constant with annotation
@deprecated("Use NEW_CONST instead")
const OLD_CONST = 42;

/// Global variable with annotation
@external
global EXTERNAL_VAR: int;

/// Struct with annotation
@serializable
struct AnnotatedStruct {
    value: int
}
