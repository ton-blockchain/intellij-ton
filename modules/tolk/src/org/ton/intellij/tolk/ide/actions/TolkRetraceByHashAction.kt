package org.ton.intellij.tolk.ide.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.remoting.ActionRemoteBehaviorSpecification
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.ui.InputValidator
import com.intellij.openapi.ui.Messages
import org.ton.intellij.tolk.debug.retrace.TolkRetraceLauncher

class TolkRetraceByHashAction :
    AnAction("Retrace Transaction by Hash"),
    ActionRemoteBehaviorSpecification.Frontend {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        LOG.info("TolkRetraceByHashAction invoked for project '${project.name}'")

        val hash = Messages.showInputDialog(
            project,
            "Transaction hash to retrace",
            "Tolk Retrace",
            null,
            "",
            object : InputValidator {
                override fun checkInput(inputString: String?): Boolean = !inputString.isNullOrBlank()

                override fun canClose(inputString: String?): Boolean = checkInput(inputString)
            },
        )?.trim() ?: return
        TolkRetraceLauncher.launch(project, hash)
    }

    companion object {
        private val LOG = logger<TolkRetraceByHashAction>()
    }
}
