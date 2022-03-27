package com.github.andreypfau.intellijton.notification

import com.github.andreypfau.intellijton.Plugin
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NotificationPostStartupActivity :
    StartupActivity.Background,
    StartupActivity.DumbAware,
    CoroutineScope,
    Disposable {

    private val parentJob = SupervisorJob()
    override val coroutineContext  = Dispatchers.Default + parentJob

    override fun runActivity(project: Project) {
        launch {
            checkUpdate()
        }
    }

    override fun dispose() {
        parentJob.cancel()
    }

    private fun checkUpdate() {
        val version = Plugin.version
        if (version != null)
    }
}