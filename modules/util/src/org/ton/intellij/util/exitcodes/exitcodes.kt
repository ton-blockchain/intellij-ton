package org.ton.intellij.util.exitcodes

data class ExitCodeInfo(
    val origin: String,
    val description: String,
    val short: String,
)

fun generateExitCodeDocumentation(code: Int): String? {
    val info = DATA[code] ?: return null

    return """
        ${info.description}

        **Phase**: ${info.origin}

        Learn more about exit codes in documentation: https://docs.ton.org/v3/documentation/tvm/tvm-exit-codes
    """.trimIndent()
}

fun generateShortExitCodeDocumentation(code: Int): String? {
    val info = DATA[code] ?: return null
    return info.short
}

val DATA: Map<Int, ExitCodeInfo> = mapOf(
    0 to ExitCodeInfo(
        origin = "Compute and action phases",
        description = "Standard successful execution exit code.",
        short = "Successful execution"
    ),
    1 to ExitCodeInfo(
        origin = "Compute phase",
        description = "Alternative successful execution exit code. Reserved, but doesn’t occur.",
        short = "Alternative success (reserved)"
    ),
    2 to ExitCodeInfo("Compute phase", "Stack underflow.", "Stack underflow"),
    3 to ExitCodeInfo("Compute phase", "Stack overflow.", "Stack overflow"),
    4 to ExitCodeInfo("Compute phase", "Integer overflow.", "Integer overflow"),
    5 to ExitCodeInfo(
        "Compute phase",
        "Range check error — some integer is out of its expected range.",
        "Range check error"
    ),
    6 to ExitCodeInfo("Compute phase", "Invalid TVM opcode.", "Invalid opcode"),
    7 to ExitCodeInfo("Compute phase", "Type check error.", "Type check error"),
    8 to ExitCodeInfo("Compute phase", "Cell overflow.", "Cell overflow"),
    9 to ExitCodeInfo("Compute phase", "Cell underflow.", "Cell underflow"),
    10 to ExitCodeInfo("Compute phase", "Dictionary error.", "Dictionary error"),
    11 to ExitCodeInfo(
        "Compute phase",
        "Unknown error, may be thrown by user programs.",
        "Unknown error"
    ),
    12 to ExitCodeInfo(
        "Compute phase",
        "Fatal error. Thrown by TVM in situations deemed impossible.",
        "Fatal error"
    ),
    13 to ExitCodeInfo("Compute phase", "Out of gas error.", "Out of gas"),
    -14 to ExitCodeInfo(
        "Compute phase",
        "Same as 13. Negative, so that it cannot be faked.",
        "Out of gas (negative)"
    ),
    14 to ExitCodeInfo(
        "Compute phase",
        "VM virtualization error. Reserved, but never thrown.",
        "Virtualization error (reserved)"
    ),
    32 to ExitCodeInfo("Action phase", "Action list is invalid.", "Invalid action list"),
    33 to ExitCodeInfo("Action phase", "Action list is too long.", "Action list too long"),
    34 to ExitCodeInfo("Action phase", "Action is invalid or not supported.", "Unsupported action"),
    35 to ExitCodeInfo(
        "Action phase",
        "Invalid source address in outbound message.",
        "Invalid source address"
    ),
    36 to ExitCodeInfo(
        "Action phase",
        "Invalid destination address in outbound message.",
        "Invalid destination address"
    ),
    37 to ExitCodeInfo("Action phase", "Not enough Toncoin.", "Insufficient Toncoin"),
    38 to ExitCodeInfo("Action phase", "Not enough extra currencies.", "Insufficient extra currencies"),
    39 to ExitCodeInfo(
        "Action phase",
        "Outbound message does not fit into a cell after rewriting.",
        "Message too big for cell"
    ),
    40 to ExitCodeInfo(
        "Action phase",
        "Cannot process a message — not enough funds, the message is too large or its Merkle depth is too big.",
        "Cannot process message"
    ),
    41 to ExitCodeInfo(
        "Action phase",
        "Library reference is null during library change action.",
        "Null library reference"
    ),
    42 to ExitCodeInfo("Action phase", "Library change action error.", "Library change error"),
    43 to ExitCodeInfo(
        "Action phase",
        "Exceeded maximum number of cells in the library or the maximum depth of the Merkle tree.",
        "Library size/depth exceeded"
    ),
    50 to ExitCodeInfo("Action phase", "Account state size exceeded limits.", "Account state too large")
)
