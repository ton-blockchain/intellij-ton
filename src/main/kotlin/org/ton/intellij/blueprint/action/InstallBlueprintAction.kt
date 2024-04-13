package org.ton.intellij.blueprint.action

import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterManager
import com.intellij.lang.javascript.JavaScriptBundle
import com.intellij.lang.javascript.modules.InstallNodeModuleQuickFix
import com.intellij.lang.javascript.modules.PackageInstaller
import com.intellij.notification.Notification
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import java.io.File

const val BLUEPRINT_PKG = "@ton-community/blueprint"

class InstallBlueprintAction(
    val project: Project,
    val packageJson: VirtualFile,
    val notification: Notification? = null,
) : DumbAwareAction(
    JavaScriptBundle.message(
        "node.js.quickfix.install.node.module.with.dev.dependencies.text",
        BLUEPRINT_PKG
    )
) {
    override fun actionPerformed(e: AnActionEvent) {
        val interpreter = NodeJsInterpreterManager.getInstance(project).interpreter ?: return
        val parent = packageJson.parent
        notification?.hideBalloon()

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Installing TON-Blueprint") {
            override fun run(indicator: ProgressIndicator) {
                val listener = InstallNodeModuleQuickFix.createListener(project, packageJson, BLUEPRINT_PKG)
                val extraOptions = InstallNodeModuleQuickFix.buildExtraOptions(project, true)

                PackageInstaller(project, interpreter, BLUEPRINT_PKG, null, File(parent.path), listener, extraOptions)
                    .run(indicator)

                VfsUtil.markDirtyAndRefresh(true, false, false, packageJson)
            }
        })
    }
}
