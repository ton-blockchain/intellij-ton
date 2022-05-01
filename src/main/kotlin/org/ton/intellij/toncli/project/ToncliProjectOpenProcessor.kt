package org.ton.intellij.toncli.project

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupManager
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.PlatformProjectOpenProcessor
import com.intellij.projectImport.ProjectOpenProcessor
import org.ton.intellij.toncli.ToncliConstants
import org.ton.intellij.toncli.icons.ToncliIcons
import org.ton.intellij.toncli.project.model.guessAndSetupToncliProject
import javax.swing.Icon

class ToncliProjectOpenProcessor : ProjectOpenProcessor() {

    override fun getIcon(): Icon = ToncliIcons.ICON
    override fun getName(): String = "toncli"

    override fun canOpenProject(file: VirtualFile): Boolean {
        return FileUtil.namesEqual(file.name, ToncliConstants.MANIFEST_FILE) ||
                file.isDirectory && file.findChild(ToncliConstants.MANIFEST_FILE) != null
    }

    override fun doOpenProject(virtualFile: VirtualFile, projectToClose: Project?, forceNewFrame: Boolean): Project? {
        val basedir = if (virtualFile.isDirectory) virtualFile else virtualFile.parent

        return PlatformProjectOpenProcessor.getInstance().doOpenProject(basedir, projectToClose, forceNewFrame)?.also {
            StartupManager.getInstance(it).runWhenProjectIsInitialized { guessAndSetupToncliProject(it) }
        }
    }
}