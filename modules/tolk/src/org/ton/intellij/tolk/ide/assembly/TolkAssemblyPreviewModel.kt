package org.ton.intellij.tolk.ide.assembly

internal data class TolkAssemblyPreviewOutput(val assemblyText: String, val blocks: List<TolkAssemblyPreviewBlock>)

data class TolkAssemblyPreviewBlock(val sourceLines: IntRange, val assemblyLines: List<IntRange>)

internal data class TolkCompileJsonResult(
    val success: Boolean = false,
    val code_boc64: String? = null,
    val error: String? = null,
    val errors: List<TolkCompileDiagnostic>? = null,
) {
    val errorMessage: String?
        get() = errors?.takeIf { it.isNotEmpty() }?.joinToString("\n\n") { it.render() } ?: error
}

/** Source diagnostics from `acton compile --json`; location-free failures have a null range. */
internal data class TolkCompileDiagnostic(
    val message: String,
    val range: TolkCompileDiagnosticRange? = null,
    val in_function: String? = null,
    val secondary_locations: List<TolkCompileRelatedLocation>? = null,
) {
    fun render(): String = buildString {
        range?.let { append("${it.file_name}:${it.start_line_no}:${it.start_char_no}: ") }
        append(message)
        in_function?.let { append("\n$it") }
        secondary_locations.orEmpty().forEach { note ->
            append("\nnote: ${note.note}")
            note.range?.let { append(" (${it.file_name}:${it.start_line_no}:${it.start_char_no})") }
        }
    }
}

internal data class TolkCompileDiagnosticRange(val file_name: String, val start_line_no: Int, val start_char_no: Int)

internal data class TolkCompileRelatedLocation(val note: String, val range: TolkCompileDiagnosticRange? = null)

internal data class TolkDisasmJsonResult(
    val success: Boolean = false,
    val assembly: String? = null,
    val blocks: List<TolkDisasmJsonBlock> = emptyList(),
    val error: String? = null,
)

internal data class TolkDisasmJsonBlock(
    val source: TolkDisasmJsonSourceLocation? = null,
    val assembly_ranges: List<TolkDisasmJsonRange> = emptyList(),
)

internal data class TolkDisasmJsonSourceLocation(
    val file: String,
    val line: Int,
    val column: Int,
    val end_line: Int,
    val end_column: Int,
)

internal data class TolkDisasmJsonRange(val start_line: Int, val end_line: Int)

data class TolkAssemblyPreviewPresentation(
    val status: TolkAssemblyPreviewStatus,
    val blocks: List<TolkAssemblyPreviewBlock> = emptyList(),
) {
    companion object {
        fun loading(): TolkAssemblyPreviewPresentation = TolkAssemblyPreviewPresentation(
            status = TolkAssemblyPreviewStatus.Loading,
        )
    }
}

sealed interface TolkAssemblyPreviewStatus {
    data object Loading : TolkAssemblyPreviewStatus
    data object Ready : TolkAssemblyPreviewStatus
    data class Failed(val message: String) : TolkAssemblyPreviewStatus
}
