package org.ton.intellij.acton.profiler

import com.intellij.execution.configurations.RunProfile
import com.intellij.profiler.clion.ProfilerRunChecker
import org.ton.intellij.acton.runconfig.ActonCommandConfiguration

class ActonProfilerRunChecker : ProfilerRunChecker {
    override fun isPerfProfilerCanBeUsed(): Boolean = true

    override fun isDTraceProfilerCanBeUsed(): Boolean = false

    override fun isProfilerCompatible(profile: RunProfile): Boolean {
        return profile is ActonCommandConfiguration && profile.command == "test"
    }
}
