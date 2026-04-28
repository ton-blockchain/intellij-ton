package org.ton.intellij.tolk.ide.test

import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ProjectViewNode
import com.intellij.ide.projectView.ProjectViewNodeDecorator
import com.intellij.ui.JBColor
import java.awt.Color

class TolkTestsProjectViewNodeDecorator : ProjectViewNodeDecorator {
    override fun decorate(node: ProjectViewNode<*>, data: PresentationData) {
        val project = node.project
        val file = node.virtualFile ?: return
        if (file.isTestsRoot(project)) {
            data.background = TEST_ROOT_BACKGROUND
            data.setIcon(AllIcons.Modules.TestRoot)
            return
        }
        if (file.isUnderTestsRoot(project)) {
            data.background = TEST_ROOT_BACKGROUND
            return
        }
        if (file.isSourceRoot(project)) {
            data.setIcon(AllIcons.Modules.SourceRoot)
        }
    }

    companion object {
        private val TEST_ROOT_BACKGROUND = JBColor(
            Color(0xEAF7EC),
            Color(0x203226),
        )
    }
}
