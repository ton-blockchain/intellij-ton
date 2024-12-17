package org.ton.intellij.fc2tolk.tree

fun <E : FTTreeElement, D> applyRecursive(element: E, data: D, func: (FTTreeElement, D) -> FTTreeElement): E {
    val iterator = element.children.listIterator()

    fun applyRecursiveToList(child: List<FTTreeElement>): List<FTTreeElement> {
        val newChild = child.map { func(it, data) }
        child.forEach { it.detach(element) }
        iterator.set(child)
        newChild.forEach { it.attach(element) }
        return newChild
    }

    while (iterator.hasNext()) {
        when (val child = iterator.next()) {
            is List<*> -> {
                @Suppress("UNCHECKED_CAST")
                val newChild = applyRecursiveToList(child as List<FTTreeElement>)
                iterator.set(newChild)
            }

            is FTTreeElement -> {
                val newChild = func(child, data)
                if (child !== newChild) {
                    child.detach(element)
                    iterator.set(newChild)
                    newChild.attach(element)
                }
            }

            else -> error("unsupported child type: ${child::class}")
        }
    }

    return element
}

private val VALID_IDENTIFIER_REGEX = Regex("^[a-zA-Z_$][a-zA-Z_$0-9]*$")

private val TOLK_KEYWORDS = setOf(
    "return",
    "var",
    "repeat",
    "do",
    "while",
    "try",
    "catch",
    "if",
    "else",
    "int",
    "cell",
    "slice",
    "builder",
    "continuation",
    "tuple",
    "type",
    "global",
    "asm",
    "operator",
    "infix",
    "const",
    "true",
    "false",
    "null",
    "builtin",
    "get",
    "import",
    "fun",
    "redef",
    "auto",
    "mutate",
    "assert",
    "throw",
    "void",
    "self",
    "tolk",
    "val",
    "bool",
    "enum",
    "struct",
    "export",
    "break",
    "continue"
)

fun String.escapedTolkId(): String {
    val onlyUnderscores = isNotEmpty() && this.count { it == '_' } == length
    return if (onlyUnderscores || !VALID_IDENTIFIER_REGEX.matches(this) || TOLK_KEYWORDS.contains(this)) "`$this`" else this
}