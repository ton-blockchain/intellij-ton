package org.ton.intellij.acton.ide

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import fleet.util.logging.logger

class ActonExternalLinterResult(
    val commandOutput: String,
    val executionTime: Long,
) {
    val diagnostics: List<ActonDiagnostic> = try {
        LOG.info("external linter took: ${executionTime}ms")
        GSON.fromJson(commandOutput, ActonLinterResponse::class.java)?.diagnostics?.filter { it.file != null } ?: emptyList()
    } catch (e: Exception) {
        LOG.error(e, "Failed to parse external linter output")
        emptyList()
    }

    companion object {
        private val LOG = logger<ActonExternalLinterResult>()
        private val GSON = Gson()
    }
}

private data class ActonLinterResponse(
    val diagnostics: List<ActonDiagnostic>? = null,
)

data class ActonDiagnostic(
    val file: String?,
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("severity")
    private val _severity: String? = null,
    @SerializedName("message")
    private val _message: String? = null,
    @SerializedName("annotations")
    private val _annotations: List<ActonAnnotation>? = null,
    @SerializedName("fixes")
    private val _fixes: List<ActonFix>? = null,
    @SerializedName("source")
    private val _source: String? = null,
) {
    val severity: String get() = _severity ?: "error"
    val message: String get() = _message ?: ""
    val annotations: List<ActonAnnotation> get() = _annotations?.filter { it.isValid } ?: emptyList()
    val fixes: List<ActonFix> get() = _fixes?.filter { it.isValid } ?: emptyList()
    val source: String get() = _source ?: "tolk"
}

data class ActonAnnotation(
    @SerializedName("range")
    private val _range: ActonRange? = null,
    @SerializedName("message")
    private val _message: String? = null,
    @SerializedName("is_primary")
    private val _isPrimary: Boolean? = null,
) {
    val range: ActonRange get() = _range!!
    val message: String? get() = _message
    val isPrimary: Boolean get() = _isPrimary ?: true
    val isValid: Boolean get() = _range != null
}

data class ActonRange(
    val start: ActonPosition,
    val end: ActonPosition,
)

data class ActonPosition(
    val line: Int,
    val character: Int,
)

data class ActonFix(
    @SerializedName("message")
    private val _message: String? = null,
    @SerializedName("edits")
    private val _edits: List<ActonEdit>? = null,
    @SerializedName("applicability")
    private val _applicability: ActonApplicability? = null,
) {
    val message: String get() = _message ?: ""
    val edits: List<ActonEdit> get() = _edits?.filter { it.isValid } ?: emptyList()
    val applicability: ActonApplicability get() = _applicability ?: ActonApplicability.Auto
    val isValid: Boolean get() = _edits != null
}

enum class ActonApplicability {
    @SerializedName("auto")
    Auto,

    @SerializedName("manual")
    Manual
}

data class ActonEdit(
    @SerializedName("range")
    private val _range: ActonRange? = null,
    @SerializedName("newText")
    private val _newText: String? = null,
) {
    val range: ActonRange get() = _range!!
    val newText: String get() = _newText ?: ""
    val isValid: Boolean get() = _range != null
}
