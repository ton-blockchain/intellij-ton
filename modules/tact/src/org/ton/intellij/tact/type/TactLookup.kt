package org.ton.intellij.tact.type

import com.intellij.openapi.project.Project
import org.ton.intellij.tact.psi.TactElement

class TactLookup(
    private val project: Project,
    context: TactElement? = null
) {
    val ctx by lazy(LazyThreadSafetyMode.NONE) {
        TactInferenceContext(project, this)
    }
}
