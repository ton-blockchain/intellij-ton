/// Simple assembly function
fun simpleAsm(x: int): int asm "ADD";

/// Assembly function with multiple parameters
fun complexAsm(a: int, b: int, c: slice): int asm(a b c) "ROT" "ADD" "SWAP";

/// Assembly function with indices
fun asmWithIndices(): slice asm( -> 0 1) "PUSHSLICE x{deadbeef}";

/// Method with assembly implementation
fun Point.distance(self): int asm "SUB" "ABS";
