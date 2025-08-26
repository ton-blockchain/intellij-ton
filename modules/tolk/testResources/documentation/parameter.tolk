/// Function to test parameter documentation
fun testParameters(
    /// Simple parameter
    simple: int,
    /// Mutable parameter  
    mutate mutable: slice,
    /// Parameter with default value
    withDefault: bool = true,
    /// Generic parameter
    generic: T
): void {
    var localVar: int = 42;
    val constVar: slice = "hello";
}
