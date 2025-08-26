/// This is a simple doc comment
/// 
/// It can have multiple lines and **markdown** formatting.
/// 
/// # Examples
/// 
/// ```tolk
/// someFunction(42);
/// ```
/// 
/// # Parameters
/// 
/// - `param1` - The first parameter
/// - `param2` - The second parameter
/// 
/// # Returns
/// 
/// Returns the sum of the parameters.
fun documentedFunction(param1: int, param2: int): int {
    return param1 + param2;
}

/// Struct with rich documentation
/// 
/// This struct represents a 2D point in space.
/// 
/// ## Usage
/// 
/// ```tolk
/// var p = Point { x: 10, y: 20 };
/// ```
struct Point {
    /// X coordinate in pixels
    x: int,
    /// Y coordinate in pixels  
    y: int
}

/// Constant with documentation
/// 
/// This constant defines the maximum allowed value.
/// 
/// **Warning**: Don't exceed this value!
const MAX_VALUE = 1000;
