enum EEmpty {}

enum Color {
    Red,
    Green,
    Blue
}

enum EPartialAssigned {
    M100 = -100,
    M99,
    P1 = (1 + 0) << 0,
    P2 = ((+1 + 1) << 1) / 2,
    P3,
    P4,
    P5 = 0b101,
    P6 = SIX,
    Max = 0x7FFFFFFF,
}

enum Color: uint8 {
    Red,
}
