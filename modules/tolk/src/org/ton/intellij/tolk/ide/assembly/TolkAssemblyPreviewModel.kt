package org.ton.intellij.tolk.ide.assembly

internal data class TolkAssemblyPreviewOutput(val assemblyText: String, val blocks: List<TolkAssemblyPreviewBlock>)

data class TolkAssemblyPreviewBlock(val sourceLines: IntRange, val assemblyLines: List<IntRange>)

internal data class TolkCompileJsonResult(
    val success: Boolean = false,
    val code_boc64: String? = null,
    val error: String? = null,
)

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
