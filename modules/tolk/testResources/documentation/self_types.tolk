/// Struct for testing self types
struct Calculator {
    value: int
}

/// Method with self parameter
fun Calculator.add(self, other: int): self {
    return Calculator { value: self.value + other };
}

/// Mutating method
fun Calculator.increment(mutate self): void {
    self.value += 1;
}

/// Method returning self type
fun Calculator.reset(self): self {
    return Calculator { value: 0 };
}

/// Static method (no self)
fun Calculator.create(initial: int): Calculator {
    return Calculator { value: initial };
}
