package org.ton.intellij.acton.runconfig

import com.intellij.execution.testframework.sm.runner.SMTestProxy
import com.intellij.execution.testframework.stacktrace.DiffHyperlink
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import java.util.concurrent.ConcurrentHashMap

val Project.actonTestFailureState: ActonTestFailureStateService
    get() = service<ActonTestFailureStateService>()

@Service(Service.Level.PROJECT)
class ActonTestFailureStateService {
    private val comparisonFailures = ConcurrentHashMap<String, ComparisonFailure>()

    fun getComparisonFailure(locationUrl: String?): ComparisonFailure? =
        locationUrl?.takeUnless(String::isBlank)?.let(comparisonFailures::get)

    fun update(test: SMTestProxy) {
        val locationUrl = test.locationUrl ?: return
        val failure = comparisonFailure(test)
        if (failure == null) {
            comparisonFailures.remove(locationUrl)
        } else {
            comparisonFailures[locationUrl] = failure
        }
    }

    fun clear(locationUrl: String?) {
        if (locationUrl.isNullOrBlank()) return
        comparisonFailures.remove(locationUrl)
    }

    companion object {
        internal fun comparisonFailure(test: SMTestProxy): ComparisonFailure? =
            comparisonFailure(test.diffViewerProvider ?: test.diffViewerProviders.firstOrNull())

        private fun comparisonFailure(diffHyperlink: DiffHyperlink?): ComparisonFailure? {
            if (diffHyperlink == null) return null
            return ComparisonFailure(
                actual = diffHyperlink.right,
                expected = diffHyperlink.left,
            )
        }
    }
}

data class ComparisonFailure(val actual: String, val expected: String)
