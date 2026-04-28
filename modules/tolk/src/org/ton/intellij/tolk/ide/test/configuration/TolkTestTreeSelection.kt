package org.ton.intellij.tolk.ide.test.configuration

import com.intellij.execution.testframework.AbstractTestProxy
import com.intellij.openapi.vfs.VfsUtilCore
import org.ton.intellij.acton.cli.ActonCommand

internal object TolkTestTreeSelection {
    internal fun resolveSelection(test: AbstractTestProxy): Selection? {
        val location = TolkTestLocator.parseLocationUrl(test.locationUrl)
        if (location != null) {
            return Selection(
                mode = ActonCommand.Test.TestMode.FUNCTION,
                target = location.filePath,
                functionName = location.functionName,
                displayName = "Test ${location.functionName}",
            )
        }

        val locationUrl = test.locationUrl ?: return null
        if (!locationUrl.startsWith("file://")) return null

        return Selection(
            mode = ActonCommand.Test.TestMode.FILE,
            target = VfsUtilCore.urlToPath(locationUrl),
            functionName = "",
            displayName = "Test ${test.name}",
        )
    }

    internal data class Selection(
        val mode: ActonCommand.Test.TestMode,
        val target: String,
        val functionName: String,
        val displayName: String,
    )
}
