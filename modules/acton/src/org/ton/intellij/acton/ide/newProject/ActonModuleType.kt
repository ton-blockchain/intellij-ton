package org.ton.intellij.acton.ide.newProject

import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.module.ModuleTypeManager
import org.ton.intellij.acton.ActonIcons
import javax.swing.Icon

class ActonModuleType : ModuleType<ActonModuleBuilder>(ID) {
    override fun createModuleBuilder(): ActonModuleBuilder = ActonModuleBuilder()

    override fun getName(): String = "TON"

    override fun getDescription(): String = "TON project"

    override fun getNodeIcon(isOpened: Boolean): Icon = ActonIcons.TON

    companion object {
        const val ID = "ACTON_MODULE"
        val INSTANCE: ActonModuleType by lazy {
            ModuleTypeManager.getInstance().findByID(ID) as ActonModuleType
        }
    }
}
