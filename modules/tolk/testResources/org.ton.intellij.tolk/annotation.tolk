struct Point {
    x: int,
    y: int
}

fun main() {
    var p = Point {}
    p.toCell({
        skipBitsNFieldsValidation: 12
    })
}
